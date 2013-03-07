package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.AlignmentBoardDataBuilder;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/13/13
 * Time: 3:21 PM
 *
 * Takes on all tasks for pushing data into the Alignment Board Viewer, given its base entity.
 */
public class ABLoadWorker extends SimpleWorker {

    private AlignmentBoardContext context;
    private Mip3d mip3d;
    private AlignmentBoardViewer viewer;
    private RenderMappingI renderMapping;
    private Logger logger;

    public ABLoadWorker(
            AlignmentBoardViewer viewer, AlignmentBoardContext context, Mip3d mip3d, RenderMappingI renderMapping
    ) {
        logger = LoggerFactory.getLogger( ABLoadWorker.class );
        this.context = context;
        this.mip3d = mip3d;
        this.viewer = viewer;
        this.renderMapping = renderMapping;
    }

    @Override
    protected void doStuff() throws Exception {

        mip3d.clear();

        logger.info( "In load thread, before getting bean list." );
        Collection<RenderableBean> renderableBeans =
                new AlignmentBoardDataBuilder()
                    .setAlignmentBoardContext( context )
                        .getRenderableBeanList();

        if ( renderableBeans == null  ||  renderableBeans.size() == 0 ) {
            logger.info( "No renderables found for alignment board " + context.getName() );
            mip3d.clear();
        }
        else {
            logger.info( "In load thread, after getting bean list." );

            Collection<RenderableBean> signalRenderables = getSignalRenderables(renderableBeans);

            FileResolver resolver = new CacheFileResolver();
            final CyclicBarrier barrier = new CyclicBarrier( signalRenderables.size() + 1 );
            for ( RenderableBean signalRenderable: signalRenderables ) {
//                loadVolume(renderableBeans, resolver, signalRenderable);

                // Multithreaded load.
                LoadRunnable runnable = new LoadRunnable( renderableBeans, resolver, signalRenderable, barrier );
                new Thread( runnable ).start();
            }

            try {
                barrier.await();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }

        // Strip any "show-loading" off the viewer.
        viewer.removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        viewer.add( mip3d, BorderLayout.CENTER );

        logger.info( "Ending load thread." );
    }

    @Override
    protected void hadSuccess() {
        viewer.revalidate();
        viewer.repaint();

        mip3d.refresh();

    }

    @Override
    protected void hadError(Throwable error) {
        viewer.removeAll();
        viewer.revalidate();
        viewer.repaint();
        SessionMgr.getSessionMgr().handleException( error );
    }

    private void loadVolume(Collection<RenderableBean> renderableBeans, FileResolver resolver, RenderableBean signalRenderable) {
        logger.info("In load thread, STARTING load of volume " + new java.util.Date());

        String signalFilename = signalRenderable.getSignalFile();
        Collection<String> labelFiles = getLabelsForSignalFile(renderableBeans, signalFilename);
        Collection<RenderableBean> nextSignalsRenderables = getRenderables(renderableBeans, signalFilename);
        mip3d.setMaskColorMappings(renderMapping.getMapping(nextSignalsRenderables));

        VolumeMaskBuilder volumeMaskBuilder = createMaskBuilder(
                labelFiles, nextSignalsRenderables, resolver
        );

        if ( volumeMaskBuilder == null ) {
            float[] rgb = new float[] { 1.0f, 0.3f, 0.3f };  // Default value.
            if ( signalRenderable.getRgb() != null ) {
                byte[] signalByteColors = signalRenderable.getRgb();
                if ( signalByteColors[ 3 ] == RenderMappingI.NON_RENDERING ) {
                    for ( int i = 0; i < rgb.length; i++ ) {
                        rgb[ i ] = 0.0f;
                    }
                }
                else {
                    for ( int i = 0; i < rgb.length; i++ ) {
                        int signalIntColor = signalByteColors[ i ];
                        if ( signalByteColors[ i ] < 0 ) {
                            signalIntColor = 256 + signalIntColor;
                        }
                        rgb[ i ] = signalIntColor / 255.0f;
                    }
                }
            }
            mip3d.loadVolume( signalFilename, rgb, resolver );
        }
        else {
            mip3d.loadVolume( signalFilename, volumeMaskBuilder, resolver );
        }

        logger.info("In load thread, ENDED load of volume.");
    }

    /**
     * Extract the subset of renderable beans that applies to the signal filename given.
     *
     * @param signalFilename find things pertaining to this signal file.
     * @return all the renderables which refer to this signal file.
     */
    private Collection<RenderableBean> getRenderables(Collection<RenderableBean> renderableBeans, String signalFilename) {
        Collection<RenderableBean> rtnVal = new HashSet<RenderableBean>();
        for ( RenderableBean bean: renderableBeans ) {
            if ( bean.getSignalFile() != null && bean.getSignalFile().equals( signalFilename ) ) {
                rtnVal.add( bean );
            }
        }
        return rtnVal;
    }

    /**
     * Extract the set of signal file names from the renderable beans.
     *
     * @return all signal file names found in any bean.
     */
    private Collection<RenderableBean> getSignalRenderables(Collection<RenderableBean> renderableBeans) {
        Collection<RenderableBean> signalFileNames = new HashSet<RenderableBean>();
        for ( RenderableBean bean: renderableBeans ) {
            if ( bean.isSignal() ) {
                signalFileNames.add( bean );
            }
        }
        return signalFileNames;
    }

    /**
     * Extract the mask file names from the renderable beans.
     *
     *
     * @param signalFilename which signal to find label file names against.
     * @return all label file names from beans which refer to the signal file name given.
     */
    private Collection<String> getLabelsForSignalFile(Collection<RenderableBean> renderableBeans, String signalFilename) {
        Collection<String> rtnVal = new HashSet<String>();
        for ( RenderableBean bean: renderableBeans ) {
            if ( bean.getSignalFile() != null  &&
                 bean.getSignalFile().equals( signalFilename )  &&
                 bean.getLabelFile() != null ) {
                rtnVal.add( bean.getLabelFile() );
            }
        }
        return rtnVal;
    }

    /**
     * Accumulates all data for masking, from the set of files provided, preparing them for
     * injection into the volume being loaded.
     *
     * @param labelFiles all label files found in the renderables.
     * @param renderables all items which may be rendered in this volume.
     * @param resolver for finding true paths of files.
     */
    private VolumeMaskBuilder createMaskBuilder(
            Collection<String> labelFiles, Collection<RenderableBean> renderables, FileResolver resolver
    ) {

        VolumeMaskBuilder volumeMaskBuilder = null;
        // Build the masking texture info.
        if (labelFiles != null  &&  labelFiles.size() > 0) {
            VolumeMaskBuilder builder = new VolumeMaskBuilder();
            builder.setRenderables(renderables);
            for ( String labelFile: labelFiles ) {
                VolumeLoader volumeLoader = new VolumeLoader( resolver );
                if ( volumeLoader.loadVolume(labelFile) ) {
                    volumeLoader.populateVolumeAcceptor(builder);
                }
            }
            volumeMaskBuilder = builder;
        }

        return volumeMaskBuilder;
    }

    public class LoadRunnable implements Runnable {
        private Collection<RenderableBean> renderableBeans;
        private RenderableBean signalRenderable;
        private FileResolver resolver;
        private CyclicBarrier barrier;

        public LoadRunnable(
                Collection<RenderableBean> renderableBeans, FileResolver resolver, RenderableBean signalRenderable,
                CyclicBarrier barrier
        ) {
            this.renderableBeans = renderableBeans;
            this.resolver = resolver;
            this.signalRenderable = signalRenderable;
            this.barrier = barrier;
        }

        public void run() {
            ABLoadWorker.this.loadVolume(renderableBeans, resolver, signalRenderable);
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

