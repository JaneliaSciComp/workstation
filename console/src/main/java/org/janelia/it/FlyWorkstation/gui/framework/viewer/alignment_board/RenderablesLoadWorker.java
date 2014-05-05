package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.FragmentSizeSetterAndFilter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.GpuSampler;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.RenderablesChannelsBuilder;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.RenderablesMaskBuilder;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.jacs.shared.loader.MaskChanStreamSource;
import org.janelia.it.jacs.shared.loader.mesh.SurfaceOnlyAcceptorDecorator;
import org.janelia.it.jacs.shared.loader.mesh.VoxelSurfaceCollector;
import org.janelia.it.jacs.shared.loader.mesh.VoxelSurfaceCollectorFactory;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.loader.FileStats;
import org.janelia.it.jacs.shared.loader.MaskChanDataAcceptorI;
import org.janelia.it.jacs.shared.loader.MaskChanMultiFileLoader;
import org.janelia.it.jacs.shared.loader.file_resolver.FileResolver;
import org.janelia.it.jacs.shared.loader.renderable.*;
import org.janelia.it.jacs.shared.loader.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 11:16 AM
 *
 * Loads renderable-oriented data into the Alignment Board and MIP3d.
 */
public class RenderablesLoadWorker extends SimpleWorker implements RenderableDataLoader {

    private static final boolean HOLLOW_RENDERING = false;
    private static final int LEAST_FULLSIZE_MEM = 1500000; // Ex: 1,565,620
    private Boolean loadFiles = true;

    private MaskChanMultiFileLoader compartmentLoader;
    private MaskChanMultiFileLoader neuronFragmentLoader;
    private RenderMappingI renderMapping;
    private FileStats fileStats;

    private RenderablesMaskBuilder maskTextureBuilder;
    private RenderablesChannelsBuilder signalTextureBuilder;
    private RenderableDataSourceI dataSource;
    private AlignmentBoardSettings alignmentBoardSettings;
    private MultiMaskTracker multiMaskTracker;

    private AlignmentBoardControllable controlCallback;
    private GpuSampler sampler;

    private FileResolver resolver;

    private Logger logger;

    public RenderablesLoadWorker(
            RenderableDataSourceI dataSource,
            RenderMappingI renderMapping,
            AlignmentBoardControllable controlCallback,
            AlignmentBoardSettings settings,
            MultiMaskTracker multiMaskTracker
    ) {
        logger = LoggerFactory.getLogger(RenderablesLoadWorker.class);
        this.dataSource = dataSource;
        this.renderMapping = renderMapping;
        this.alignmentBoardSettings = settings;
        this.controlCallback = controlCallback;
        this.multiMaskTracker = multiMaskTracker;
    }

    @Override
    public void propertyChange( PropertyChangeEvent e ) {
        if (progressMonitor==null) return;
        if ("progress".equals(e.getPropertyName())) {
            int progress = (Integer) e.getNewValue();
            progressMonitor.setProgress(progress);
            if (progressMonitor.isCanceled()) {
                super.cancel( true );
            }
        }
    }

    /**
     * This c'tor to be called when the downsample rate must be determined from graphic board.
     * @throws Exception for called methods. Particularly threading.
     */
    public RenderablesLoadWorker(
            RenderableDataSourceI dataSource,
            RenderMappingI renderMapping,
            AlignmentBoardControllable controlCallback,
            AlignmentBoardSettings settings,
            MultiMaskTracker multiMaskTracker,
            GpuSampler sampler
    ) throws Exception {
        this( dataSource, renderMapping, controlCallback, settings, multiMaskTracker );
        this.sampler = sampler;
    }

    public void setResolver( FileResolver resolver ) {
        this.resolver = resolver;
    }

    public void setLoadFilesFlag( Boolean loadFiles ) {
        this.loadFiles = loadFiles;
    }

    public void setFileStats(FileStats fileStats) {
        this.fileStats = fileStats;
    }

    //------------------------------------------IMPLEMENTS RenderableDataLoader
    /**
     * Loads one renderable's data into the volume under construction.
     *
     * @param maskChanRenderableData renderable data to be applied to volume.
     * @throws Exception from called methods.
     */
    public void loadRenderableData( MaskChanRenderableData maskChanRenderableData ) throws Exception {
        logger.debug(
                "In load thread, STARTING load of renderable {}.",
                maskChanRenderableData.getBean().getTranslatedNum()
        );

        // Special case: the "signal" renderable will have a translated label number of zero.  It will not
        // require a file load.
        if ( maskChanRenderableData.getBean().getTranslatedNum() == 0 ) {
            return;
        }

        MaskChanStreamSource streamSource = new MaskChanStreamSource(
                maskChanRenderableData, resolver, alignmentBoardSettings.isShowChannelData()
        );

        MaskChanStreamSource.StreamSourceSanity sanity = streamSource.getSanity();
        if ( ! sanity.isSane() ) {
            logger.warn( sanity.getMessage() );
            return;
        }

        // Feed data to the acceptors.
        if ( maskChanRenderableData.isCompartment() ) {
            compartmentLoader.read(maskChanRenderableData.getBean(), streamSource);
        }
        else {
            neuronFragmentLoader.read(maskChanRenderableData.getBean(), streamSource);
        }

        logger.debug("In load thread, ENDED load of renderable {}.", maskChanRenderableData.getBean().getLabelFileNum() );
    }


    //----------------------------------------------OVERRIDE SimpleWorker
    @Override
    protected void doStuff() throws Exception {

        if (dataSource==null) return;
        Collection<MaskChanRenderableData> renderableDatas = dataSource.getRenderableDatas();

        // Cut down the to-renders: (at time-of-writing) use only the larger ones.
        Collection<MaskChanRenderableData> originalDatas = new ArrayList<MaskChanRenderableData>( renderableDatas );
        long fragmentFilterSize = alignmentBoardSettings.getMinimumVoxelCount();
        long fragmentCutoffCount = alignmentBoardSettings.getMaximumNeuronCount();
        if ( fragmentFilterSize != AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT  ||
             fragmentCutoffCount != AlignmentBoardSettings.NO_NEURON_SIZE_CONSTRAINT ) {
            FragmentSizeSetterAndFilter filter = new FragmentSizeSetterAndFilter( fragmentFilterSize, fragmentCutoffCount );
            renderableDatas = filter.filter( renderableDatas );
        }

        if ( loadFiles ) {
            // Handoff latest info from load, for fallbacks in coloring.
            SessionMgr.getBrowser().getLayersPanel().setFileStats( fileStats );

            if ( ! checkpoint( "Setting up..." ) ) {
                return;
            }

            if ( resolver == null ) {
                //resolver = new TrivialFileResolver();  // swap comments, in testing.
                resolver = new CacheFileResolver();
            }

            if ( sampler != null )
                alignmentBoardSettings = adjustDownsampleRateSetting();

            boolean passedCheckPointAfterFilter = filterCheckboxes(renderableDatas, originalDatas);
            if ( ! passedCheckPointAfterFilter )
                return;

            List<RenderableBean> renderableBeans = new ArrayList<RenderableBean>();
            int lastUsedMask = extractRenderableBeansFromRenderableDatas( renderableDatas, renderableBeans );
            if ( lastUsedMask > -1 ) {
                multiMaskTracker.setFirstMaskNum( lastUsedMask + 1 ); // Add one to move past all allocated masks.
            }
            Collections.sort( renderableBeans, new InvertingComparator( new RBComparator() ) );

            renderMapping.setRenderables( renderableBeans );

            if ( ! checkpoint( "Loading voxel data." ) ) {
                return;
            }

            neuronFragmentLoader = new MaskChanMultiFileLoader();
            compartmentLoader = new MaskChanMultiFileLoader();

            //buildNothing(renderableDatas, renderableBeans);

            buildMaskVolume(renderableDatas, renderableBeans);
            buildSignalVolume(renderableDatas, renderableBeans);

            compartmentLoader.close();
            neuronFragmentLoader.close();
        }
        else {
            renderChange(renderableDatas);
        }

        logger.debug("Ending load thread.");
    }

    @Override
    protected void hadSuccess() {
        controlCallback.loadCompletion(true, loadFiles, null);
    }

    @Override
    protected void hadError(Throwable error) {
        controlCallback.loadCompletion(false, loadFiles, error);
    }

    /**
     * Carries out all file-reading.
     *
     * @param metaDatas one thread for each of these.
     */
    private void multiThreadedDataLoad(Collection<MaskChanRenderableData> metaDatas, boolean buildTexture) {
        controlCallback.clearDisplay();

        if ( metaDatas == null  ||  metaDatas.size() == 0 ) {
            logger.info( "No renderables found for alignment board " + dataSource.getName() );
        }
        else {
            logger.debug( "In load thread, after getting bean list." );

            logger.debug("Starting multithreaded file load.");
            fileLoad(metaDatas, buildTexture);
            if ( buildTexture ) {
                if ( ( getProgressMonitor() == null )  ||  (! getProgressMonitor().isCanceled() ) ) {
                    logger.debug("Starting multithreaded texture build.");
                    multiThreadedTextureBuild();
                }
            }

        }

        if ( ( getProgressMonitor() == null )  ||  (! getProgressMonitor().isCanceled() ) )
            controlCallback.displayReady();
    }

    /**
     * Carry out the texture building in parallel.  One thread for each texture type.
     */
    private void multiThreadedTextureBuild() {
        // Multi-threading, part two.  Here, the renderable textures are created out of inputs.
        final CyclicBarrier buildBarrier = new CyclicBarrier( 3 );
        boolean successful = true;
        try {
            if (! checkpoint( "Assembling final data" ) )
                return;

            // These two texture-build steps will proceed in parallel.
            TexBuildRunnable signalBuilderRunnable = new TexBuildRunnable( signalTextureBuilder, buildBarrier );
            TexBuildRunnable maskBuilderRunnable = new TexBuildRunnable( maskTextureBuilder, buildBarrier );

            new Thread( signalBuilderRunnable ).start();
            new Thread( maskBuilderRunnable ).start();

            buildBarrier.await();

            if ( buildBarrier.isBroken() ) {
                getProgressMonitor().close();
                throw new Exception( "Tex build failed." );
            }

            if ( getProgressMonitor() != null ) {
                double downSampleRate = alignmentBoardSettings.getAcceptedDownsampleRate();
                boolean continueFlag = true;
                if ( downSampleRate > 1.0 ) {
                    continueFlag = checkpoint("Downsampling / " + alignmentBoardSettings.getAcceptedDownsampleRate());
                }
                else {
                    continueFlag = checkpoint("Pushing final data");
                }
                if ( ! continueFlag ) {
                    return;
                }
            }


            TextureDataI signalTexture = signalBuilderRunnable.getTextureData();
            TextureDataI maskTexture = maskBuilderRunnable.getTextureData();

            controlCallback.loadVolume(signalTexture, maskTexture);

        } catch ( BrokenBarrierException bbe ) {
            successful = false;
            logger.error( "Barrier await failed during texture build.", bbe );
            bbe.printStackTrace();
        } catch ( InterruptedException ie ) {
            successful = false;
            logger.error( "Thread interrupted during texture build.", ie );
            ie.printStackTrace();
        } catch ( Exception ex ) {
            successful = false;
            logger.error( "Exception during texture build.", ex );
            ex.printStackTrace();
        } finally {
            if ( buildBarrier.isBroken()  ||  !successful ) {
                getProgressMonitor().close();
                throw new RuntimeException("Failed to push data to Alignment Board.");
            }
            else {
                buildBarrier.reset(); // Signal to others: stop.
            }
        }
    }

    /**
     * Eliminate checkboxes from the Layers Panel, for things that will never be rendered because they have been
     * filtered.
     *
     * @param renderableDatas product of this operation
     * @param originalDatas raw list.
     * @return true if the checkpoints were passed, prior-to/during this operation.  False otherwise.
     */
    private boolean filterCheckboxes(
            Collection<MaskChanRenderableData> renderableDatas, Collection<MaskChanRenderableData> originalDatas
    ) {
        // Go through the original list.  Anything not in the filtered list must be marked as excluded;
        // anything remaining on the list is un-marked excluded.
        Map<RenderableBean,MaskChanRenderableData> idToData = new HashMap<RenderableBean,MaskChanRenderableData>();
        for ( MaskChanRenderableData data: renderableDatas ) {
            idToData.put( data.getBean(), data );
        }
        SessionMgr.getBrowser().getLayersPanel().showLoadingIndicator();
        for ( MaskChanRenderableData data: originalDatas ) {
            if ( ! checkpoint( "Filtering checkboxes." ) ) {
                return false;
            }

            MaskChanRenderableData targetData = idToData.get(data.getBean());
            RenderableBean bean = data.getBean();
            if ( bean != null  &&
                    bean.getRenderableEntity() != null  &&
                    bean.getType().equals( EntityConstants.TYPE_NEURON_FRAGMENT )
                    ) {

                AlignedItem item = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext().getAlignedItemWithEntityId(bean.getAlignedItemId());
                if ( item != null ) {
                    try {
                        if ( targetData != null ) {
                            // It's in the filtered list.  Mark it as such.
                            item.setInclusionStatus(AlignedItem.InclusionStatus.In);
                        }
                        else {
                            // Not in the filtered list.  Exlude it.
                            item.setInclusionStatus(AlignedItem.InclusionStatus.ExcludedForSize);
                        }
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                        logger.error("Failing to set inclusion status for entity id=" + bean.getRenderableEntity().getId() );
                    }
                }
            }
        }
        return true;
    }

    private Map<Integer,VoxelSurfaceCollector> collectorMap;

    /**
     * Builds out the signal volume from the files referred to by renderables.  Sets up common code for the
     * "signal" mode.
     *
     * @param renderableDatas sources of data paths.
     * @param renderableBeans sources of mask, coloring, etc.
     */
    private void buildSignalVolume(
            Collection<MaskChanRenderableData> renderableDatas, List<RenderableBean> renderableBeans
    ) {
        // Establish the means for extracting the signal data.
        signalTextureBuilder = new RenderablesChannelsBuilder(
                alignmentBoardSettings, multiMaskTracker, maskTextureBuilder, renderableBeans
        );

        if ( checkpoint( "Preparing display" ) ) {
            ArrayList<MaskChanDataAcceptorI> signalDataAcceptors = new ArrayList<MaskChanDataAcceptorI>();
            if ( HOLLOW_RENDERING ) {
                ensureCollectorMap(renderableDatas);
                // RE-run the scan.  This time only the signal-texture-builder will accept the data.
                SurfaceOnlyAcceptorDecorator soDecorator =
                        new SurfaceOnlyAcceptorDecorator( signalTextureBuilder, collectorMap );

                signalDataAcceptors.add(soDecorator);
            }
            else {
                signalDataAcceptors.add(signalTextureBuilder);
            }
            neuronFragmentLoader.setAcceptors(signalDataAcceptors);
            compartmentLoader.setAcceptors( signalDataAcceptors );

            logger.debug("Timing multi-thread data load for signal.");
            multiThreadedDataLoad(renderableDatas, true);
            logger.debug("End timing signal load");

        }
    }

    /**
     * Builds out the mask volume from the files referred to by the renderables.  Sets up common code for the
     * "mask" mode.
     *
     * @param renderableDatas sources of data paths.
     * @param renderableBeans sources of mask numbers.
     */
    private void buildMaskVolume(
            Collection<MaskChanRenderableData> renderableDatas, List<RenderableBean> renderableBeans
    ) {
        // Establish the means for extracting the volume mask.
        maskTextureBuilder = new RenderablesMaskBuilder( alignmentBoardSettings, renderableBeans );

        // Unfortunately, the wrapped thing knows what wraps it, but at least by a different interface.
        RemaskingAcceptorDecorator remaskingAcceptorDecorator = new RemaskingAcceptorDecorator(
                maskTextureBuilder,
                multiMaskTracker,
                maskTextureBuilder,
                RenderablesMaskBuilder.UNIVERSAL_MASK_BYTE_COUNT,
                false    // NOT binary / search writeback.  That happens only for the file writeback code.
        );

        ArrayList<MaskChanDataAcceptorI> maskDataAcceptors = new ArrayList<MaskChanDataAcceptorI>();
        if ( HOLLOW_RENDERING ) {
            ensureCollectorMap(renderableDatas);
            SurfaceOnlyAcceptorDecorator soDecorator =
                    new SurfaceOnlyAcceptorDecorator( remaskingAcceptorDecorator, collectorMap );

            maskDataAcceptors.add( soDecorator );
        }
        else {
            maskDataAcceptors.add( remaskingAcceptorDecorator );
        }

        // Setup the loader to traverse all this data on demand. Only the mask-tex-builder accepts data.
        neuronFragmentLoader.setAcceptors(maskDataAcceptors);
        neuronFragmentLoader.setFileStats(fileStats);

        compartmentLoader.setAcceptors(maskDataAcceptors);
        compartmentLoader.setFileStats(fileStats);

        logger.debug("Timing multi-thread data load for multi-mask-assbembly.");
        multiThreadedDataLoad(renderableDatas, false);
        logger.debug("End timing multi-mask-assembly");
        maskDataAcceptors.clear();
    }

    private synchronized void ensureCollectorMap(Collection<MaskChanRenderableData> renderableDatas) {
        if ( collectorMap == null ) {
            collectorMap = new HashMap<Integer,VoxelSurfaceCollector>();
            VoxelSurfaceCollectorFactory voxelSurfaceCollectorFactory = new VoxelSurfaceCollectorFactory( resolver, alignmentBoardSettings.isShowChannelData() );
            for ( MaskChanRenderableData renderableData: renderableDatas ) {
                try {
                    VoxelSurfaceCollector collector = voxelSurfaceCollectorFactory.getSurfaceCollector(renderableData);
                    collectorMap.put( renderableData.getBean().getTranslatedNum(), collector );
                } catch ( Exception ex ) {
                    throw new RuntimeException( ex );
                }
            }
        }
    }

    private void fileLoad( Collection<MaskChanRenderableData> metaDatas, boolean buildTexture ) {
        List<MaskChanRenderableData> sortedMetaDatas = new ArrayList<MaskChanRenderableData>();
        sortedMetaDatas.addAll( metaDatas );
        Collections.sort( sortedMetaDatas, new RDComparator( false ) );
        int i = 0;
        String msgPrefix = buildTexture ? "Building " : "Examining ";
        for ( MaskChanRenderableData metaData: sortedMetaDatas ) {
            logger.debug( "Scheduling mask path {} for load as {}.", metaData.getMaskPath(), metaData.getBean().getTranslatedNum() );
            LoadRunnable runnable = new LoadRunnable( metaData, this, null );

            // Here, can modify the progress bar.
            if ( getProgressMonitor() != null ) {
                i++;
                if ( ! checkpoint(msgPrefix + i + " of " + sortedMetaDatas.size() + " items.") ) {
                    return;  // Jump out if canceled at checkpoint.
                }
            }

            runnable.run();
        }

    }

    /**
     *  Establish the "uncovered bean list".
     *
     *  @param renderableDatas contain renderable beans plus file paths.
     *  @param renderableBeans just the beans.  This colleciton will be populated as an OUTPUT of this method.
     *  @return the highest mask number found in any renderable bean.
     */
    private int extractRenderableBeansFromRenderableDatas(Collection<MaskChanRenderableData> renderableDatas, List<RenderableBean> renderableBeans) {
        int lastUsedMask = -1;
        for ( MaskChanRenderableData renderableData: renderableDatas ) {
            RenderableBean bean = renderableData.getBean();
            renderableBeans.add( bean );
            if ( bean.getTranslatedNum() > lastUsedMask ) {
                lastUsedMask = bean.getTranslatedNum();
            }
        }
        return lastUsedMask;
    }

    private boolean previouslyClosed = false;   // Flag to ensure absence of progress monitor does not thwart cancel.
    private boolean checkpoint( String checkPointNote ) {
        boolean rtnVal = true;
        ProgressMonitor pm = getProgressMonitor();
        if ( pm != null  ||  previouslyClosed ) {
            if ( previouslyClosed  ||  pm.isCanceled() ) {
                previouslyClosed = true;
                this.cancel( true );
                SessionMgr.getBrowser().getLayersPanel().showNothing();
                controlCallback.clearDisplay();
                controlCallback.close();
                rtnVal = false;
                logger.warn("Bailed at checkpoint {}.", checkPointNote);
                pm.close();
            }
            else {
                pm.setNote( checkPointNote );
            }
        }
        return rtnVal;
    }

    /**
     * Allows the downsample-rate setting used in populating the textures, to be adjusted based on user's
     * platform.
     *
     * @return adjusted settings.
     * @throws Exception from any called methods.
     */
    private AlignmentBoardSettings adjustDownsampleRateSetting() throws Exception {

        logger.debug("Adjusting downsample rate from {}.", Thread.currentThread().getName());
        try {
            GpuSampler.GpuInfo gpuInfo = sampler.getGpuInfo();

            // Must set the down sample rate to the newly-discovered best.
            if ( gpuInfo != null ) {
                logger.info(
                        "GPU vendor {}, renderer {} version " + gpuInfo.getVersion(), gpuInfo.getVender(), gpuInfo.getRenderer()
                );

                // 1.5Gb in Kb increments
                logger.debug( "ABV seeing free memory estimate of {}.", gpuInfo.getFreeTexMem() );
                logger.debug( "ABV seeing highest supported version of {}.", gpuInfo.getHighestGlslVersion() );

                if ( gpuInfo.getFreeTexMem() > LEAST_FULLSIZE_MEM ) {
                    alignmentBoardSettings.setDownSampleGuess(1.0);
                }
                else if ( GpuSampler.isDeptStandardGpu( gpuInfo.getRenderer() ) ) {
                    alignmentBoardSettings.setDownSampleGuess(1.0);
                }
                else {
                    Future<Boolean> isDeptPreferred = sampler.isDepartmentStandardGraphicsMac();
                    try {
                        if ( isDeptPreferred.get() ) {
                            logger.info("User has preferred card.");
                            alignmentBoardSettings.setDownSampleGuess(1.0);
                        }
                        else {
                            alignmentBoardSettings.setDownSampleGuess(2.0);
                        }
                    } catch ( Exception ex ) {
                        logger.warn( "Ignore this message if this system is not a Mac: department-preferred grapchics detection not working on this platform." );
                    }
                }
            }
            else {
                logger.warn( "No vender data returned.  Forcing 'safe guess'." );
                alignmentBoardSettings.setDownSampleGuess(2.0);
            }

        } catch ( Exception ex ) {
            ex.printStackTrace();
            SessionMgr.getSessionMgr().handleException( ex );
        }

        //todo find some way to return this and avoid re-processing.
        //cachedDownSampleGuess = alignmentBoardSettings.getDownSampleGuess();
        return alignmentBoardSettings;
    }

    private void renderChange(Collection<MaskChanRenderableData> metaDatas) {
        if ( metaDatas.size() == 0 ) {
            logger.info("No renderables found for alignment board " + dataSource.getName());
        }
        else {
            Collection<RenderableBean> beans = new ArrayList<RenderableBean>();
            for ( MaskChanRenderableData metaData: metaDatas ) {
                beans.add( metaData.getBean() );
            }
            renderMapping.setRenderables( beans );
            controlCallback.renderModCompletion();
        }

    }

}
