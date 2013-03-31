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

    private static final float GAMMA_VALUE = 1.0f;
    private Mip3d mip3d;
    private JComponent viewer;
    private Boolean loadFiles = true;

    private MaskChanMultiFileLoader compartmentLoader;
    private MaskChanMultiFileLoader neuronFragmentLoader;
    private RenderMappingI renderMapping;

    private RenderablesMaskBuilder vmb;
    private RenderablesChannelsBuilder vcb;
    private RenderableDataSourceI dataSource;

    private FileResolver resolver;

    private Logger logger;

    public RenderablesLoadWorker(
            JComponent container, RenderableDataSourceI dataSource, Mip3d mip3d
    ) {
        logger = LoggerFactory.getLogger(RenderablesLoadWorker.class);
        this.dataSource = dataSource;
        this.mip3d = mip3d;
        this.viewer = container;
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

        renderMapping = new ConfigurableColorMapping();
        renderMapping.setRenderables( renderableBeans );

        /* Establish all volume builders for this test. */
        ArrayList<MaskChanDataAcceptorI> acceptors = new ArrayList<MaskChanDataAcceptorI>();

        // Establish the means for extracting the volume mask.
        vmb = new RenderablesMaskBuilder();
        vmb.setRenderables(renderableBeans);

        // Establish the means for extracting the signal data.
        vcb = new RenderablesChannelsBuilder();

        // Setup the loader to traverse all this data on demand.
        neuronFragmentLoader = new MaskChanMultiFileLoader();
        acceptors.add(vmb);
        acceptors.add(vcb);
        neuronFragmentLoader.setAcceptors(acceptors);

        compartmentLoader = new MaskChanMultiFileLoader();
        compartmentLoader.setAcceptors(Arrays.<MaskChanDataAcceptorI>asList(vmb));

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

    private void multiThreadedDataLoad(Collection<MaskChanRenderableData> metaDatas) {
        mip3d.clear();

        if ( metaDatas == null  ||  metaDatas.size() == 0 ) {
            logger.info( "No renderables found for alignment board " + dataSource.getName() );
            mip3d.clear();
        }
        else {
            logger.info( "In load thread, after getting bean list." );

            if ( resolver == null ) {
                resolver = new CacheFileResolver();
            }

            final CyclicBarrier barrier = new CyclicBarrier( metaDatas.size() + 1 );
            String filename = null;
            for ( MaskChanRenderableData metaData: metaDatas ) {
                // Multithreaded load.
                LoadRunnable runnable = new LoadRunnable( resolver, metaData, barrier );
                new Thread( runnable ).start();
            }

            try {
                barrier.await();

                if ( barrier.isBroken() ) {
                    throw new Exception( "Load failed." );
                }
                else {
                    compartmentLoader.close();
                    neuronFragmentLoader.close();

                    // The volume's data is loaded here.
                    if ( ! mip3d.setVolume( vcb, vmb, renderMapping, GAMMA_VALUE )) {
                        logger.error( "Failed to load volume to mip3d." );
                    }
                }

            } catch ( BrokenBarrierException bbe ) {
                logger.error( "Barrier await failed during loading " + filename, bbe );
                bbe.printStackTrace();
            } catch ( InterruptedException ie ) {
                logger.error( "Thread interrupted during loading " + filename, ie );
                ie.printStackTrace();
            } catch ( Exception ex ) {
                logger.error( "Exception during loading " + filename, ex );
                ex.printStackTrace();
            } finally {
                if ( ! barrier.isBroken() ) {
                    barrier.reset(); // Signal to others: failed.
                }
            }

        }

        mip3d.refresh();

        // Strip any "show-loading" off the viewer.
        viewer.removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        viewer.add(mip3d, BorderLayout.CENTER);
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
        logger.info(
                "In load thread, STARTING load of renderable {}.",
                maskChanRenderableData.getBean().getTranslatedNum()
        );

        // Special case: the "signal" renderable may have no mask or channel file.
        if ( maskChanRenderableData.getMaskPath() == null   &&   maskChanRenderableData.getChannelPath() != null ) {
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

        // Only the neuron fragment renderables are relevant to this load.  The signal renderable
        // is kept separate.  Hence skip the first (0th) renderable.
        System.out.println( "Reading " + maskChanRenderableData.getMaskPath() );

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

        logger.info("In load thread, ENDED load of renderable {}.", maskChanRenderableData.getBean().getLabelFileNum() );
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

}
