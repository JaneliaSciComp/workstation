package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
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

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 11:16 AM
 *
 * Loads renderable-oriented data into the Alignment Board and MIP3d.
 */
public class RenderablesLoadWorker extends SimpleWorker {

    private static final float GAMMA_VALUE = 0.5f;
    private Mip3d mip3d;
    private JComponent viewer;
    private Boolean loadFiles = true;

    private MaskChanMultiFileLoader compartmentLoader;
    private MaskChanMultiFileLoader neuronFragmentLoader;
    private RenderMappingI renderMapping;

    private RenderablesMaskBuilder maskTextureBuilder;
    private RenderablesChannelsBuilder signalTextureBuilder;
    private RenderableDataSourceI dataSource;
    private double downSampleRate;

    private FileResolver resolver;

    private Logger logger;

    public RenderablesLoadWorker(
            JComponent container,
            RenderableDataSourceI dataSource,
            Mip3d mip3d,
            RenderMappingI renderMapping,
            double downSampleRate
    ) {
        logger = LoggerFactory.getLogger(RenderablesLoadWorker.class);
        this.dataSource = dataSource;
        this.mip3d = mip3d;
        this.viewer = container;
        this.renderMapping = renderMapping;
        this.downSampleRate = downSampleRate;
    }

    public void setResolver( FileResolver resolver ) {
        this.resolver = resolver;
    }

    public void setLoadFilesFlag( Boolean loadFiles ) {
        this.loadFiles = loadFiles;
    }

    @Override
    protected void doStuff() throws Exception {

        logger.info( "In load thread, before getting bean list." );
        Collection<MaskChanRenderableData> renderableDatas = dataSource.getRenderableDatas();

        Collection<RenderableBean> renderableBeans = new ArrayList<RenderableBean>();
        for ( MaskChanRenderableData renderableData: renderableDatas ) {
            renderableBeans.add( renderableData.getBean() );
        }

        renderMapping.setRenderables( renderableBeans );

        /* Establish all volume builders for this test. */
        ArrayList<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();

        // Establish the means for extracting the volume mask.
        maskTextureBuilder = new RenderablesMaskBuilder( downSampleRate );
        maskTextureBuilder.setRenderables(renderableBeans);

        // Establish the means for extracting the signal data.
        signalTextureBuilder = new RenderablesChannelsBuilder( downSampleRate );

        // Setup the loader to traverse all this data on demand.
        neuronFragmentLoader = new MaskChanMultiFileLoader();
        acceptors.add(maskTextureBuilder);
        acceptors.add(signalTextureBuilder);
        neuronFragmentLoader.setAcceptors(acceptors);

        compartmentLoader = new MaskChanMultiFileLoader();
        compartmentLoader.setAcceptors(Arrays.<MaskChanDataAcceptorI>asList(maskTextureBuilder));

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
        viewer.revalidate();
        viewer.repaint();

        if ( loadFiles ) {
            mip3d.refresh();
        }
        else {
            mip3d.refreshRendering();
        }

    }

    @Override
    protected void hadError(Throwable error) {
        viewer.removeAll();
        viewer.revalidate();
        viewer.repaint();
        SessionMgr.getSessionMgr().handleException(error);
    }

    /**
     * Carries out all file-reading in parallel.
     *
     * @param metaDatas one thread for each of these.
     */
    private void multiThreadedDataLoad(Collection<MaskChanRenderableData> metaDatas) {
        mip3d.clear();

        if ( metaDatas == null  ||  metaDatas.size() == 0 ) {
            logger.info( "No renderables found for alignment board " + dataSource.getName() );
            mip3d.clear();
        }
        else {
            logger.info( "In load thread, after getting bean list." );

            if ( resolver == null ) {
                resolver = new TrivialFileResolver();  // todo swap comments, in production.
                //resolver = new CacheFileResolver();
            }

            multiThreadedFileLoad( metaDatas );

            compartmentLoader.close();
            neuronFragmentLoader.close();

            multiThreadedTextureBuild();


        }

        mip3d.refresh();

        // Strip any "show-loading" off the viewer.
        viewer.removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        viewer.add(mip3d, BorderLayout.CENTER);
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


            if ( ! mip3d.setVolume( signalTexture, maskTexture, renderMapping, GAMMA_VALUE ) ) {
                logger.error( "Failed to load volume to mip3d." );
            }

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

    private void multiThreadedFileLoad(Collection<MaskChanRenderableData> metaDatas) {
        final CyclicBarrier loadBarrier = new CyclicBarrier( metaDatas.size() + 1 );
        for ( MaskChanRenderableData metaData: metaDatas ) {
            // Multithreaded load.
            LoadRunnable runnable = new LoadRunnable( resolver, metaData, loadBarrier );
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
        }

    }

    private void loadVolume( FileResolver resolver, MaskChanRenderableData maskChanRenderableData ) throws Exception {
        logger.debug(
                "In load thread, STARTING load of renderable {}.",
                maskChanRenderableData.getBean().getTranslatedNum()
        );

        // Special case: the "signal" renderable may have no mask or channel file.
        if ( maskChanRenderableData.getMaskPath() == null   ||   maskChanRenderableData.getChannelPath() == null ) {
            logger.warn(
                    "Renderable {} has either a missing mask or channel file -- {}.",
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

        InputStream maskStream =
                new BufferedInputStream(
                        new FileInputStream( resolver.getResolvedFilename( maskChanRenderableData.getMaskPath() )
                        )
                );
        InputStream chanStream =
                new BufferedInputStream(
                        new FileInputStream( resolver.getResolvedFilename( maskChanRenderableData.getChannelPath() )
                        )
                );

        //System.out.println( "Reading " + maskChanRenderableData.getMaskPath() );

        if ( maskChanRenderableData.isCompartment() )  {
            // Iterating through these files will cause all the relevant data to be loaded into
            // the acceptors, which here includes only the mask builder.
            compartmentLoader.read(maskChanRenderableData.getBean(), maskStream, chanStream);
        }
        else {
            // Iterating through these files will cause all the relevant data to be loaded into
            // the acceptors, which here are the mask builder and the channels builder.
            neuronFragmentLoader.read(maskChanRenderableData.getBean(), maskStream, chanStream);
        }

        maskStream.close();
        chanStream.close();

        logger.debug("In load thread, ENDED load of renderable {}.", maskChanRenderableData.getBean().getLabelFileNum() );
    }

    public RenderMappingI getRenderMapping() {
        return renderMapping;
    }

    public class LoadRunnable implements Runnable {
        private MaskChanRenderableData metaData;
        private FileResolver resolver;
        private CyclicBarrier barrier;

        public LoadRunnable(
                FileResolver resolver, MaskChanRenderableData signalRenderable,
                CyclicBarrier barrier
        ) {
            this.resolver = resolver;
            this.metaData = signalRenderable;
            this.barrier = barrier;
        }

        public void run() {
            try {
                RenderablesLoadWorker.this.loadVolume(resolver, metaData);
            } catch ( Exception ex ) {
                ex.printStackTrace();
                barrier.reset();   // This tells others that the barrier is broken.
                throw new RuntimeException( ex );
            }

            try {
                barrier.await();
            } catch ( BrokenBarrierException bbe ) {
                bbe.printStackTrace();
            } catch ( InterruptedException ie ) {
                ie.printStackTrace();
            }
        }
    }

    /**
     * Simply makes a texture builder build its texture, inside a thread.  Once complete, the getter for the
     * texture data may be called.  Not before.
     */
    public class TexBuildRunnable implements Runnable {
        private TextureBuilderI textureBuilder;
        private CyclicBarrier barrier;
        private TextureDataI textureData;

        public TexBuildRunnable( TextureBuilderI textureBuilder, CyclicBarrier barrier ) {
            this.textureBuilder = textureBuilder;
            this.barrier = barrier;
        }

        public TextureDataI getTextureData() {
            return textureData;
        }

        public void run() {
            try {
                textureData = textureBuilder.buildTextureData();
                barrier.await();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }
    }

}
