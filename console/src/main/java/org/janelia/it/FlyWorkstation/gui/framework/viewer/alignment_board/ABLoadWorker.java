package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ConfigurableColorMapping;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.MaskBuilderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.SampleData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.AlignmentBoardDataBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.util.*;
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

    private static final float GAMMA_VALUE = 1.5f;
    private AlignmentBoardContext context;
    private Mip3d mip3d;
    private AlignmentBoardViewer viewer;
    private Boolean loadFiles = true;
    private Logger logger;

    private Map<Long,RenderMappingI> renderMappings;
    private CropCoordSet cropCoordSet;

    public ABLoadWorker(
            AlignmentBoardViewer viewer, AlignmentBoardContext context, Mip3d mip3d
    ) {
        this( viewer, context, mip3d, new HashMap<Long,RenderMappingI>(), null );
    }

    public ABLoadWorker(
            AlignmentBoardViewer viewer,
            AlignmentBoardContext context,
            Mip3d mip3d,
            Map<Long,RenderMappingI> renderMappings,
            CropCoordSet cropCoordSet
    ) {
        logger = LoggerFactory.getLogger( ABLoadWorker.class );
        this.context = context;
        this.mip3d = mip3d;
        this.viewer = viewer;
        this.renderMappings = renderMappings;
        this.cropCoordSet = cropCoordSet;
    }

    public void setLoadFilesFlag( Boolean loadFiles ) {
        this.loadFiles = loadFiles;
    }

    public Map<Long,RenderMappingI> getRenderMappings() {
        return renderMappings;
    }

    @Override
    protected void doStuff() throws Exception {

        logger.info( "In load thread, before getting bean list." );
        Collection<SampleData> samples =
                new AlignmentBoardDataBuilder()
                        .setAlignmentBoardContext( context )
                            .getSamples();

        if ( loadFiles ) {
            multiThreadedDataLoad(samples);
        }
        else {
            multiThreadedRenderChange(samples);
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
        SessionMgr.getSessionMgr().handleException( error );
    }

    private void multiThreadedDataLoad(Collection<SampleData> samples) {
        mip3d.clear();

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
        viewer.add(mip3d, BorderLayout.CENTER);
    }

    private void multiThreadedRenderChange(Collection<SampleData> samples) {
        if ( samples == null  ||  samples.size() == 0 ) {
            logger.info("No renderables found for alignment board " + context.getName());
        }
        else {
            final CyclicBarrier barrier = new CyclicBarrier( samples.size() + 1 );
            String filename = null;
            for ( SampleData sampleData: samples ) {
                // Multithreaded render update.
                filename = sampleData.getSignalFile();
                RenderRunnable runnable = new RenderRunnable( sampleData, barrier );
                new Thread( runnable ).start();
            }

            try {
                barrier.await();
            } catch ( Exception ex ) {
                logger.error( "Barrier await failed during loading " + filename, ex );
                ex.printStackTrace();
            }
        }

    }

    private void loadRenderChange( SampleData sampleData ) {
        logger.info("In load volume, STARTING load of volume {}.", sampleData.getSignalFile());
        if ( sampleData != null ) {
            Long sampleId = sampleData.getSample().getRenderableEntity().getId();
            RenderMappingI renderMapping = renderMappings.get( sampleId );
            // NOTE: may be possible that the sample-data does NOT have associated render-mappings.
            if ( renderMapping != null ) {
                Collection<RenderableBean> renderables = sampleData.getRenderableBeans();
                for ( RenderableBean bean: renderables ) {
                    logger.debug( "Bean has rendering " +
                            bean.getRgb()[0] + "," + bean.getRgb()[1] + "," + bean.getRgb()[2] + "," + bean.getRgb()[3]
                    );
                }
                renderMapping.setRenderables( renderables );
            }
        }
        logger.info( "Completed setting of render mappings." );

    }

    private void loadVolume( FileResolver resolver, SampleData sampleData ) {
        logger.info("In load thread, STARTING load of volume {}.", sampleData.getSignalFile());

        String signalFilename = sampleData.getSignalFile();
        // As of latest, only have a single label file.  However, would like to preserve readiness to use
        // multiple label files, for future applicability.
        Collection<String> labelFiles = Arrays.asList( sampleData.getLabelFile() );

        Collection<RenderableBean> renderableBeans = sampleData.getRenderableBeans();
        MaskBuilderI volumeMaskBuilder = createMaskBuilder(
                labelFiles, renderableBeans, resolver
        );

        RenderableBean signalRenderable = sampleData.getSample();
        if ( volumeMaskBuilder == null ) {
            loadNonMaskedVolume(resolver, signalFilename, signalRenderable);
        }
        else {
            logger.info("Setting renderables. Size = {}", renderableBeans.size() );
            RenderMappingI renderMapping = new ConfigurableColorMapping();
            renderMapping.setRenderables( renderableBeans );

            // Retain the mapping given to the volume.
            renderMappings.put( sampleData.getSample().getRenderableEntity().getId(), renderMapping );

            // The volume's data is loaded here.
            if ( ! mip3d.loadVolume(
                    signalFilename, volumeMaskBuilder, resolver, renderMapping, cropCoordSet, GAMMA_VALUE
            ) ) {
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
        if ( ! mip3d.loadVolume( signalFilename, rgb, resolver, GAMMA_VALUE ) ) {
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
    private MaskBuilderI createMaskBuilder(
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

    public class RenderRunnable implements Runnable {
        private SampleData sampleData;
        private CyclicBarrier barrier;

        public RenderRunnable(
                SampleData signalRenderable,
                CyclicBarrier barrier
        ) {
            this.sampleData = signalRenderable;
            this.barrier = barrier;
        }

        public void run() {
            ABLoadWorker.this.loadRenderChange( sampleData );
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

