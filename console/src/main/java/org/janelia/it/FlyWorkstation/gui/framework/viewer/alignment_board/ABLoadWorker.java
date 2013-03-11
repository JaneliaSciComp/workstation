package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.SampleData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.AlignmentBoardDataBuilder;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
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
    private Boolean loadFiles = true;
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

    public void setLoadFilesFlag( Boolean loadFiles ) {
        this.loadFiles = loadFiles;
    }

    @Override
    protected void doStuff() throws Exception {

        mip3d.clear();

        logger.info( "In load thread, before getting bean list." );
        Collection<SampleData> samples =
                new AlignmentBoardDataBuilder()
                        .setAlignmentBoardContext( context )
                            .getSamples();

        if ( loadFiles ) {
            if ( samples == null  ||  samples.size() == 0 ) {
                logger.info( "No renderables found for alignment board " + context.getName() );
                mip3d.clear();
            }
            else {
                logger.info( "In load thread, after getting bean list." );

                FileResolver resolver = new CacheFileResolver();
                final CyclicBarrier barrier = new CyclicBarrier( samples.size() + 1 );
                String filename = null;
                for ( SampleData sampleData: samples ) {
                    // Multithreaded load.
                    filename = sampleData.getSignalFile();
                    LoadRunnable runnable = new LoadRunnable( resolver, sampleData, barrier );
                    new Thread( runnable ).start();
                }

                try {
                    barrier.await();
                } catch ( Exception ex ) {
                    logger.error( "Barrier await failed during loading " + filename, ex );
                    ex.printStackTrace();
                }
            }

            mip3d.refresh();

            // Strip any "show-loading" off the viewer.
            viewer.removeAll();

            // Add this last.  "show-loading" removes it.  This way, it is shown only
            // when it becomes un-busy.
            viewer.add( mip3d, BorderLayout.CENTER );
        }
        else {
            for ( SampleData sampleData: samples ) {
                loadRenderChange( sampleData );
            }
        }

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

    private void loadRenderChange( SampleData sampleData ) {
        logger.info( "In load of render change." );
        Map<Integer,byte[]> map = renderMapping.getMapping();
        logger.info( "End load of volume." );
//        if ( ! mip3d.changeRendering(sampleData.getSignalFile(), map) ) {
//            logger.error( "Failed to load masked volume {} to mip3d.", sampleData.getSignalFile() );
//        }
    }

    private void loadVolume( FileResolver resolver, SampleData sampleData ) {
        logger.info("In load thread, STARTING load of volume {}.", sampleData.getSignalFile());

        String signalFilename = sampleData.getSignalFile();
        // As of latest, only have a single label file.  However, would like to preserve readiness to use
        // multiple label files, for future applicability.
        Collection<String> labelFiles = Arrays.asList( sampleData.getLabelFile() );

        Collection<RenderableBean> renderableBeans = sampleData.getRenderableBeans();
        VolumeMaskBuilder volumeMaskBuilder = createMaskBuilder(
                labelFiles, renderableBeans, resolver
        );

        RenderableBean signalRenderable = sampleData.getSample();
        if ( volumeMaskBuilder == null ) {
            loadNonMaskedVolume(resolver, signalFilename, signalRenderable);
        }
        else {
            renderMapping.setRenderables( renderableBeans );
            if ( ! mip3d.loadVolume( signalFilename, volumeMaskBuilder, resolver, renderMapping.getMapping() ) ) {
                logger.error( "Failed to load masked volume {} to mip3d.", signalFilename );
            }
        }

        if ( sampleData.getReference() != null ) {
            loadNonMaskedVolume(resolver, sampleData.getReferenceFile(), sampleData.getReference());
        }

        logger.info("In load thread, ENDED load of volume.");
    }

    private void loadNonMaskedVolume(FileResolver resolver, String signalFilename, RenderableBean signalRenderable) {
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
        if ( ! mip3d.loadVolume( signalFilename, rgb, resolver ) ) {
            logger.error( "Failed to load {} to mip3d.", signalFilename );
        }
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
            volumeMaskBuilder = new VolumeMaskBuilder();
            volumeMaskBuilder.setRenderables(renderables);
            for ( String labelFile: labelFiles ) {
                VolumeLoader volumeLoader = new VolumeLoader( resolver );
                if ( volumeLoader.loadVolume(labelFile) ) {
                    volumeLoader.populateVolumeAcceptor(volumeMaskBuilder);
                }
            }
        }

        return volumeMaskBuilder;
    }

    public class LoadRunnable implements Runnable {
        private SampleData sampleData;
        private FileResolver resolver;
        private CyclicBarrier barrier;

        public LoadRunnable(
                FileResolver resolver, SampleData signalRenderable,
                CyclicBarrier barrier
        ) {
            this.resolver = resolver;
            this.sampleData = signalRenderable;
            this.barrier = barrier;
        }

        public void run() {
            ABLoadWorker.this.loadVolume(resolver, sampleData);
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

