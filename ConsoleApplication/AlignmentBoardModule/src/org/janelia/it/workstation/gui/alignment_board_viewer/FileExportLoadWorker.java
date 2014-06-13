package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.gui.alignment_board.loader.FragmentSizeSetterAndFilter;
import org.janelia.it.workstation.gui.alignment_board.loader.MaskChanDataAcceptorI;
import org.janelia.it.workstation.gui.alignment_board.loader.MaskChanMultiFileLoader;
import org.janelia.it.workstation.gui.alignment_board_viewer.gui_elements.ControlsListener;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.TextureBuilderI;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RBComparator;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RDComparator;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder.RenderablesChannelsBuilder;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_export.FilteringAcceptorDecorator;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_export.RecoloringAcceptorDecorator;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder.RenderablesMaskBuilder;
import org.janelia.it.workstation.gui.alignment_board.loader.MaskSingleFileLoader;
import org.janelia.it.workstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.workstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 11:16 AM
 *
 * Sends "loaded" data back to an output file.
 */
public class FileExportLoadWorker extends SimpleWorker implements VolumeLoader {

    public static final int ONLY_ONE_THREAD = 1;  // No thread safety, so constraining to 1-at-a-time.
    private MaskChanMultiFileLoader loader;
    private TextureBuilderI textureBuilder;
    private FileExportParamBean paramBean;

    private FileResolver resolver;

    private Logger logger;

    public FileExportLoadWorker( FileExportParamBean paramBean ) {
        logger = LoggerFactory.getLogger(FileExportLoadWorker.class);
        this.paramBean = paramBean;
        this.paramBean.exceptIfNotInit();
    }

    public void setResolver( FileResolver resolver ) {
        this.resolver = resolver;
    }

    @Override
    public void loadVolume( MaskChanRenderableData maskChanRenderableData ) throws Exception {
        logger.debug(
                "In load thread, STARTING load of renderable {}.",
                maskChanRenderableData.getBean().getTranslatedNum()
        );

        // Special case: the "signal" renderable will have a translated label number of zero.  It will not
        // require a file load.
        if ( maskChanRenderableData.getBean().getTranslatedNum() == 0 ) {
            return;
        }

        MaskChanStreamSource streamSource = new MaskChanStreamSource( maskChanRenderableData, resolver, paramBean.getMethod() == ControlsListener.ExportMethod.color );
        if ( ! streamSource.getSanity().isSane() ) {
            logger.warn( streamSource.getSanity().getMessage() );
            return;
        }

        // Iterating through these files will cause all the relevant data to be loaded into
        // the acceptors.
        loader.setDimWriteback( maskChanRenderableData.isCompartment() );
        loader.read(maskChanRenderableData.getBean(), streamSource);

        logger.debug("In load thread, ENDED load of renderable {}.", maskChanRenderableData.getBean().getLabelFileNum() );
    }

    @Override
    protected void doStuff() throws Exception {

        List<RenderableBean> renderableBeans = new ArrayList<RenderableBean>();

        // Cut down the to-renders: use only the larger ones.
        long fragmentFilterSize = paramBean.getFilterSize();
        long maxNeuronCount = paramBean.getMaxNeuronCount();
        Collection<MaskChanRenderableData> filteredRenderableDatas = paramBean.getRenderableDatas();
        if ( fragmentFilterSize != AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT  ||
             maxNeuronCount !=  AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT ) {

            FragmentSizeSetterAndFilter filter = new FragmentSizeSetterAndFilter( fragmentFilterSize, maxNeuronCount );
            filteredRenderableDatas = filter.filter( filteredRenderableDatas );
        }
        for ( MaskChanRenderableData renderableData: filteredRenderableDatas ) {
            RenderableBean bean = renderableData.getBean();
            renderableBeans.add( bean );

            // Need to add sizing data to each renderable bean prior to sorting, if not yet set.
            MaskSingleFileLoader loader = new MaskSingleFileLoader( bean );
            if ( bean.getVoxelCount() <= 0  &&  renderableData.getMaskPath() != null ) {
                File infile = new File( resolver.getResolvedFilename( renderableData.getMaskPath() ) );
                if ( infile.canRead() ) {
                    FileInputStream fis = new FileInputStream( infile );
                    long voxelCount = loader.getVoxelCount( fis );
                    fis.close();
                    bean.setVoxelCount(voxelCount);
                }
            }
        }

        Collections.sort( renderableBeans, Collections.reverseOrder( new RBComparator() ) );

        List<MaskChanRenderableData> sortedRenderableDatas = new ArrayList<MaskChanRenderableData>();
        sortedRenderableDatas.addAll( filteredRenderableDatas );
        Collections.sort( sortedRenderableDatas, new RDComparator( false ) );

        // Establish the means for extracting the volume mask.
        AlignmentBoardSettings customWritebackSettings = new AlignmentBoardSettings();
        customWritebackSettings.setChosenDownSampleRate(1.0);
        customWritebackSettings.setGammaFactor( paramBean.getGammaFactor() );

        if ( paramBean.getMethod() == ControlsListener.ExportMethod.binary ) {
            // Using only binary values.
            customWritebackSettings.setShowChannelData( false );
            textureBuilder = new RenderablesMaskBuilder( customWritebackSettings, renderableBeans, true );

            setupLoader();
            multiThreadedDataLoad( sortedRenderableDatas );

        }
        else if ( paramBean.getMethod() == ControlsListener.ExportMethod.color ) {
            // Using full color values.

            // HERE: change this like RenderablesLoadWorker, with two calls: one for mask, one for chan.
            customWritebackSettings.setShowChannelData( true );
            textureBuilder = new RenderablesChannelsBuilder(
                    customWritebackSettings, null, null, renderableBeans
            );

            setupLoader();
            multiThreadedDataLoad( sortedRenderableDatas );

        }

        logger.debug("Ending load thread.");
    }

   @Override
    protected void hadSuccess() {
        paramBean.getCallback().loadSucceeded();
    }

    @Override
    protected void hadError(Throwable error) {
        paramBean.getCallback().loadFailed( error );
    }

    private void setupLoader() {
        // Setup the loader to traverse all this data on demand.
        loader = new MaskChanMultiFileLoader();
        loader.setEnforcePadding( false ); // Do not extend dimensions of resulting volume beyond established space.
        loader.setCheckForConsistency( false );  // Do not force all files to share characteristics like channel count.
        MaskChanDataAcceptorI outerAcceptor;
        if ( paramBean.getCropCoords() == null || paramBean.getCropCoords().size() == 0 ) {
            outerAcceptor = textureBuilder;
        }
        else {
            outerAcceptor = new FilteringAcceptorDecorator( textureBuilder, paramBean.getCropCoords() );
        }
        outerAcceptor = new RecoloringAcceptorDecorator( outerAcceptor, paramBean.getGammaFactor() );

        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList(outerAcceptor) );
    }

    /**
     * Carries out all file-reading in parallel.
     *
     * @param metaDatas one thread for each of these.
     */
    private void multiThreadedDataLoad(Collection<MaskChanRenderableData> metaDatas) {

        if ( metaDatas == null  ||  metaDatas.size() == 0 ) {
            logger.info( "No renderables provided." );
        }
        else {
            logger.info( "In load thread, after getting bean list." );

            if ( resolver == null ) {
                resolver = new TrivialFileResolver();
                //resolver = new CacheFileResolver();
            }

            multiThreadedFileLoad( metaDatas, ONLY_ONE_THREAD);

            loader.close();

            buildTexture();

        }

    }

    private void buildTexture() {
        TextureDataI textureData = textureBuilder.buildTextureData();
        if ( textureData == null ) {
            throw new RuntimeException( "Null Texture Data Created." );
        }
        paramBean.getCallback().loadVolume(textureData);
    }

    /** The multi-thread load may not be used, because of upstream problems with multi-threading. */
    @SuppressWarnings("unused")
    private void multiThreadedFileLoad(Collection<MaskChanRenderableData> metaDatas) {
        // First load the compartments.
        final CyclicBarrier compartmentsLoadBarrier = new CyclicBarrier( metaDatas.size() + 1 );
        for ( MaskChanRenderableData metaData: metaDatas ) {
            // Multithreaded load.
            if ( metaData.isCompartment() ) {
                LoadRunnable runnable = new LoadRunnable( metaData, this, compartmentsLoadBarrier );
                new Thread( runnable ).start();
            }
        }

        awaitBarrier( compartmentsLoadBarrier );

        // Once all compartments have been loaded, do the neuron fragments.  This gives
        // the fragments a higher priority for volume writeback.
        final CyclicBarrier fragmentsLoadBarrier = new CyclicBarrier( metaDatas.size() + 1 );
        for ( MaskChanRenderableData metaData: metaDatas ) {
            // Multithreaded load.
            if ( ! metaData.isCompartment() ) {
                LoadRunnable runnable = new LoadRunnable( metaData, this, fragmentsLoadBarrier );
                new Thread( runnable ).start();
            }
        }

        awaitBarrier( fragmentsLoadBarrier );
    }

    private void multiThreadedFileLoad( Collection<MaskChanRenderableData> metaDatas, int maxThreads ) {
        ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
        for ( MaskChanRenderableData metaData: metaDatas ) {
            logger.debug( "Scheduling mask path {} for load.", metaData.getMaskPath() );
            LoadRunnable runnable = new LoadRunnable( metaData, this, null );
            threadPool.execute(runnable);
        }
        awaitThreadpoolCompletion( threadPool );

    }

    /** Wait until the threadpool has completed all processing. */
    private void awaitThreadpoolCompletion(ExecutorService threadPool) {
        try {
            // Now that the pools is laden, we call the milder shutdown, which lets us wait for completion of all.
            logger.debug("Awaiting shutdown.");
            threadPool.shutdown();
            threadPool.awaitTermination( 10, TimeUnit.MINUTES );
            logger.debug("Thread pool termination complete.");
        } catch ( InterruptedException ie ) {
            ie.printStackTrace();
        }
    }

    public static interface Callback {
        void loadSucceeded();
        void loadFailed( Throwable ex );
        void loadVolume( TextureDataI texture );
    }

    /**
     * This is a parameterization to the constructor for the "outer" class.
     */
    public static class FileExportParamBean {
        private Collection<MaskChanRenderableData> renderableDatas;
        private Collection<float[]> cropCoords;
        private Callback callback;
        private ControlsListener.ExportMethod method;
        private long filterSize;
        private long maxNeuronCount;
        private double gammaFactor;

        public Collection<MaskChanRenderableData> getRenderableDatas() {
            return renderableDatas;
        }

        public void setRenderableDatas(Collection<MaskChanRenderableData> renderableDatas) {
            this.renderableDatas = renderableDatas;
        }

        public Collection<float[]> getCropCoords() {
            return cropCoords;
        }

        public void setCropCoords(Collection<float[]> cropCoords) {
            this.cropCoords = cropCoords;
        }

        public Callback getCallback() {
            return callback;
        }

        public void setCallback(Callback callback) {
            this.callback = callback;
        }

        public void exceptIfNotInit() {
            if ( cropCoords == null  ||  renderableDatas == null  ||  callback == null ) {
                throw new IllegalArgumentException( "Parameters to file-export insufficient.  No nulls allowed." );
            }
        }

        public ControlsListener.ExportMethod getMethod() {
            return method;
        }

        public void setMethod(ControlsListener.ExportMethod method) {
            this.method = method;
        }

        public long getFilterSize() {
            return filterSize;
        }

        public void setFilterSize(long filterSize) {
            this.filterSize = filterSize;
        }

        public long getMaxNeuronCount() {
            return maxNeuronCount;
        }

        public void setMaxNeuronCount(long maxNeuronCount) {
            this.maxNeuronCount = maxNeuronCount;
        }

        public double getGammaFactor() {
            return gammaFactor;
        }

        public void setGammaFactor(double gammaFactor) {
            this.gammaFactor = gammaFactor;
        }
    }

    /** Await completion of all other cyclic barrier players. */
    private void awaitBarrier(CyclicBarrier loadBarrier) {
        try {
            loadBarrier.await();

            if ( loadBarrier.isBroken() ) {
                throw new Exception( "Load failed." );
            }

        } catch ( BrokenBarrierException bbe ) {
            logger.error( "Barrier await failed during file load.", bbe );
            bbe.printStackTrace();
        } catch ( InterruptedException ie ) {
            logger.error( "Thread interrupted during file load.", ie );
            ie.printStackTrace();
        } catch ( Exception ex ) {
            logger.error( "Exception during file load.", ex );
            ex.printStackTrace();
        } finally {
            if ( ! loadBarrier.isBroken() ) {
                loadBarrier.reset(); // Signal to others.
            }
        }
    }

}
