package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.*;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableDataSourceI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.TrivialFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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
public class RenderablesLoadWorker extends SimpleWorker implements VolumeLoader {

    private static final int MAX_FILE_LOAD_THREADS = 20;
    private Boolean loadFiles = true;

    private MaskChanMultiFileLoader compartmentLoader;
    private MaskChanMultiFileLoader neuronFragmentLoader;
    private RenderMappingI renderMapping;

    private RenderablesMaskBuilder maskTextureBuilder;
    private RenderablesChannelsBuilder signalTextureBuilder;
    private RenderableDataSourceI dataSource;
    private AlignmentBoardSettings alignmentBoardSettings;

    private AlignmentBoardControllable controlCallback;

    private FileResolver resolver;

    private Logger logger;

    public RenderablesLoadWorker(
            RenderableDataSourceI dataSource,
            RenderMappingI renderMapping,
            AlignmentBoardControllable controlCallback,
            AlignmentBoardSettings settings
    ) {
        logger = LoggerFactory.getLogger(RenderablesLoadWorker.class);
        this.dataSource = dataSource;
        this.renderMapping = renderMapping;
        this.alignmentBoardSettings = settings;
        this.controlCallback = controlCallback;
    }

    public void setResolver( FileResolver resolver ) {
        this.resolver = resolver;
    }

    public void setLoadFilesFlag( Boolean loadFiles ) {
        this.loadFiles = loadFiles;
    }

    //------------------------------------------IMPLEMENTS VolumeLoader
    /**
     * Loads one renderable's data into the volume under construction.
     *
     * @param maskChanRenderableData renderable data to be applied to volume.
     * @throws Exception from called methods.
     */
    public void loadVolume( MaskChanRenderableData maskChanRenderableData ) throws Exception {
        logger.debug(
                "In load thread, STARTING load of renderable {}.",
                maskChanRenderableData.getBean().getTranslatedNum()
        );

        // Mask file is always needed.
        if ( maskChanRenderableData.getMaskPath() == null ) {
            logger.warn(
                    "Renderable {} has a missing mask file. ID is {}.",
                    maskChanRenderableData.getBean().getTranslatedNum(),
                            + maskChanRenderableData.getBean().getRenderableEntity().getId()
            );
            return;
        }

        // Channel file is optional, unless channel data must be shown.
        if ( alignmentBoardSettings.isShowChannelData()  &&  maskChanRenderableData.getChannelPath() == null ) {
            logger.warn(
                    "Renderable {} has a missing channel file -- {}.",
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

        //  The mask stream is required in all cases.  But the channel path is optional.
        InputStream maskStream =
                new BufferedInputStream(
                        new FileInputStream( resolver.getResolvedFilename( maskChanRenderableData.getMaskPath() )
                        )
                );

        InputStream chanStream = null;
        if ( alignmentBoardSettings.isShowChannelData() ) {
            chanStream =
                    new BufferedInputStream(
                            new FileInputStream( resolver.getResolvedFilename( maskChanRenderableData.getChannelPath() )
                            )
                    );
        }

        // Feed data to the acceptors.
        if ( maskChanRenderableData.isCompartment() ) {
            compartmentLoader.read(maskChanRenderableData.getBean(), maskStream, chanStream);
        }
        else {
            neuronFragmentLoader.read(maskChanRenderableData.getBean(), maskStream, chanStream);
        }

        maskStream.close();
        if ( chanStream != null )
            chanStream.close();

        logger.debug("In load thread, ENDED load of renderable {}.", maskChanRenderableData.getBean().getLabelFileNum() );
    }

    public RenderMappingI getRenderMapping() {
        return renderMapping;
    }

    //----------------------------------------------OVERRIDE SimpleWorker
    @Override
    protected void doStuff() throws Exception {

        logger.info( "In load thread, before getting bean list." );
        Collection<MaskChanRenderableData> renderableDatas = dataSource.getRenderableDatas();

        Collection<RenderableBean> renderableBeans = new ArrayList<RenderableBean>();
        for ( MaskChanRenderableData renderableData: renderableDatas ) {
            renderableBeans.add(renderableData.getBean());
        }

        renderMapping.setRenderables( renderableBeans );

        /* Establish all volume builders for this test. */

        // Establish the means for extracting the volume mask.
        maskTextureBuilder = new RenderablesMaskBuilder( alignmentBoardSettings, renderableBeans );

        // Establish the means for extracting the signal data.
        signalTextureBuilder = new RenderablesChannelsBuilder( alignmentBoardSettings, renderableBeans );

        ArrayList<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();

        acceptors.add(maskTextureBuilder);
        acceptors.add(signalTextureBuilder);

        // Setup the loader to traverse all this data on demand.
        neuronFragmentLoader = new MaskChanMultiFileLoader();
        neuronFragmentLoader.setAcceptors(acceptors);

        compartmentLoader = new MaskChanMultiFileLoader();
        compartmentLoader.setAcceptors( acceptors );

        if ( loadFiles ) {
            multiThreadedDataLoad(renderableDatas);
        }
        else {
            renderChange(renderableDatas);
        }

        logger.info( "Ending load thread." );
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
     * Carries out all file-reading in parallel.
     *
     * @param metaDatas one thread for each of these.
     */
    private void multiThreadedDataLoad(Collection<MaskChanRenderableData> metaDatas) {
        controlCallback.clearDisplay();

        if ( metaDatas == null  ||  metaDatas.size() == 0 ) {
            logger.info( "No renderables found for alignment board " + dataSource.getName() );
        }
        else {
            logger.info( "In load thread, after getting bean list." );

            if ( resolver == null ) {
                //resolver = new TrivialFileResolver();  // todo swap comments, in production.
                resolver = new CacheFileResolver();
            }

            //sequentialFileLoad( metaDatas );
            multiThreadedFileLoad( metaDatas, MAX_FILE_LOAD_THREADS );

            compartmentLoader.close();
            neuronFragmentLoader.close();

            multiThreadedTextureBuild();


        }

        controlCallback.displayReady();
    }

    /**
     * Carry out the texture building in parallel.  One thread for each texture type.
     */
    private void multiThreadedTextureBuild() {
        // Multi-threading, part two.  Here, the renderable textures are created out of inputs.
        final CyclicBarrier buildBarrier = new CyclicBarrier( 3 );
        try {
            // These two texture-build steps will proceed in parallel.
            TexBuildRunnable signalBuilderRunnable = new TexBuildRunnable( signalTextureBuilder, buildBarrier );
            TexBuildRunnable maskBuilderRunnable = new TexBuildRunnable( maskTextureBuilder, buildBarrier );

            new Thread( signalBuilderRunnable ).start();
            new Thread( maskBuilderRunnable ).start();

            buildBarrier.await();

            if ( buildBarrier.isBroken() ) {
                throw new Exception( "Tex build failed." );
            }

            TextureDataI signalTexture = signalBuilderRunnable.getTextureData();
            TextureDataI maskTexture = maskBuilderRunnable.getTextureData();

            controlCallback.loadVolume(signalTexture, maskTexture);

        } catch ( BrokenBarrierException bbe ) {
            logger.error( "Barrier await failed during texture build.", bbe );
            bbe.printStackTrace();
        } catch ( InterruptedException ie ) {
            logger.error( "Thread interrupted during texture build.", ie );
            ie.printStackTrace();
        } catch ( Exception ex ) {
            logger.error( "Exception during texture build.", ex );
            ex.printStackTrace();
        } finally {
            if ( ! buildBarrier.isBroken() ) {
                buildBarrier.reset(); // Signal to others: failed.
            }
        }
    }

    private void multiThreadedFileLoad( Collection<MaskChanRenderableData> metaDatas, int maxThreads ) {
        ExecutorService compartmentsThreadPool = Executors.newFixedThreadPool( maxThreads );
        for ( MaskChanRenderableData metaData: metaDatas ) {
            logger.debug( "Scheduling mask path {} for load.", metaData.getMaskPath() );
            if ( metaData.isCompartment() ) {
                LoadRunnable runnable = new LoadRunnable( metaData, this, null );
                compartmentsThreadPool.execute( runnable );
            }
        }
        awaitThreadpoolCompletion( compartmentsThreadPool );

        ExecutorService neuronFragmentsThreadPool = Executors.newFixedThreadPool( maxThreads );
        for ( MaskChanRenderableData metaData: metaDatas ) {
            logger.debug( "Scheduling mask path {} for load.", metaData.getMaskPath() );
            if ( ! metaData.isCompartment() ) {
                LoadRunnable runnable = new LoadRunnable( metaData, this, null );
                neuronFragmentsThreadPool.execute(runnable);
            }
        }
        awaitThreadpoolCompletion( neuronFragmentsThreadPool );
    }

    /** Wait until the threadpool has completed all processing. */
    private void awaitThreadpoolCompletion(ExecutorService threadPool) {
        try {
            // Now that the pools is laden, we call the milder shutdown, which lets us wait for completion of all.
            logger.info("Awaiting shutdown.");
            threadPool.shutdown();
            threadPool.awaitTermination( 10, TimeUnit.MINUTES );
            logger.info("Thread pool termination complete.");
        } catch ( InterruptedException ie ) {
            ie.printStackTrace();
        }
    }

    //  THis is a bypass alternative to resort to in case of problems with multi-threaded file load.
    //   On 5/7/2013, I experienced a problem (cleared by bouncing IntelliJ IDEA) with multithreading.
    //   the tasks were all completing, but there was never any triggering of the "end" detection for whole pool.
    private void sequentialFileLoad( Collection<MaskChanRenderableData> metaDatas ) {
        for ( MaskChanRenderableData metaData: metaDatas ) {
            logger.debug( "Scheduling mask path {} for load.", metaData.getMaskPath() );
            LoadRunnable runnable = new LoadRunnable( metaData, this, null );
            runnable.run();
        }
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
