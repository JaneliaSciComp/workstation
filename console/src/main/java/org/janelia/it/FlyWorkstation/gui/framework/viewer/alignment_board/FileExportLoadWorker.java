package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements.ControlsListener;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderablesChannelsBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.TextureBuilderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.FilteringAcceptorDecorator;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderablesMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 11:16 AM
 *
 * Sends "loaded" data back to an output file.
 */
public class FileExportLoadWorker extends SimpleWorker implements VolumeLoader {

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

        // Mask file is always needed.
        if ( maskChanRenderableData.getMaskPath() == null ) {
            logger.warn(
                    "Renderable {} has a missing mask file -- {}.",
                    maskChanRenderableData.getBean().getTranslatedNum(),
                    maskChanRenderableData.getMaskPath() + maskChanRenderableData.getChannelPath()
            );
            return;
        }

        // Special case: the "signal" renderable will have a translated label number of zero.  It will not
        // require a file load.
        if ( maskChanRenderableData.getBean().getTranslatedNum() == 0 ) {
            return;
        }

        logger.debug( "File {} in progress.", maskChanRenderableData.getMaskPath() );
        //  The mask stream is required in all cases.  But the channel path is optional.
        InputStream maskStream =
                new BufferedInputStream(
                        new FileInputStream( resolver.getResolvedFilename( maskChanRenderableData.getMaskPath() )
                        )
                );

        InputStream channelStream = null;
        if ( paramBean.getMethod() == ControlsListener.ExportMethod.color ) {
            channelStream = new BufferedInputStream(
                    new FileInputStream(
                            resolver.getResolvedFilename( maskChanRenderableData.getChannelPath() )
                    )
            );
        }

        // Iterating through these files will cause all the relevant data to be loaded into
        // the acceptors.
        loader.read(maskChanRenderableData.getBean(), maskStream, channelStream);
        maskStream.close();

        if ( channelStream != null ) {
            channelStream.close();
        }

        logger.debug("In load thread, ENDED load of renderable {}.", maskChanRenderableData.getBean().getLabelFileNum() );
    }

    @Override
    protected void doStuff() throws Exception {

        Collection<RenderableBean> renderableBeans = new ArrayList<RenderableBean>();
        for ( MaskChanRenderableData renderableData: paramBean.getRenderableDatas() ) {
            renderableBeans.add( renderableData.getBean() );
        }

        // Establish the means for extracting the volume mask.
        AlignmentBoardSettings customWritebackSettings = new AlignmentBoardSettings();
        customWritebackSettings.setDownSampleRate(1.0);
        customWritebackSettings.setGammaFactor(1.0);

        if ( paramBean.getMethod() == ControlsListener.ExportMethod.binary ) {
            // Using only binary values.
            customWritebackSettings.setShowChannelData( false );
            textureBuilder = new RenderablesMaskBuilder( customWritebackSettings, renderableBeans, true );
        }
        else if ( paramBean.getMethod() == ControlsListener.ExportMethod.color ) {
            // Using full color values.
            customWritebackSettings.setShowChannelData( true );
            textureBuilder = new RenderablesChannelsBuilder( customWritebackSettings, renderableBeans );
        }

        // Setup the loader to traverse all this data on demand.
        loader = new MaskChanMultiFileLoader();
        loader.setEnforcePadding( false ); // Do not extend dimensions of resulting volume beyond established space.
        loader.setCheckForConsistency( false );  // Do not force all files to share characteristics like channel count.
        if ( paramBean.getCropCoords() == null || paramBean.getCropCoords().size() == 0 ) {
            loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList(textureBuilder) );
        }
        else {
            MaskChanDataAcceptorI filter = new FilteringAcceptorDecorator(textureBuilder, paramBean.getCropCoords() );
            loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList( filter ) );
        }

        multiThreadedDataLoad( paramBean.getRenderableDatas() );

        logger.info("Ending load thread.");
    }

    @Override
    protected void hadSuccess() {
        paramBean.getCallback().loadSucceeded();
    }

    @Override
    protected void hadError(Throwable error) {
        paramBean.getCallback().loadFailed( error );
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

            multiThreadedFileLoad( metaDatas );

            loader.close();

            buildTexture();

        }

    }

    private void buildTexture() {
        // These two texture-build steps will proceed in parallel.
        TextureDataI textureData = textureBuilder.buildTextureData();
        if ( textureData == null ) {
            throw new RuntimeException( "Null Texture Data Created." );
        }
        paramBean.getCallback().loadVolume(textureData);
    }

    private void multiThreadedFileLoad(Collection<MaskChanRenderableData> metaDatas) {
        final CyclicBarrier loadBarrier = new CyclicBarrier( metaDatas.size() + 1 );
        for ( MaskChanRenderableData metaData: metaDatas ) {
            // Multithreaded load.
            LoadRunnable runnable = new LoadRunnable( metaData, this, loadBarrier );
            new Thread( runnable ).start();
        }

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
    }

}
