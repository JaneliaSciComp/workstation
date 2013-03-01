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
import java.util.Map;

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

        mip3d.setClearOnLoad( true );

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
            mip3d.setMaskColorMappings(renderMapping.getMapping(renderableBeans));

            Collection<String> signalFilenames = getSignalFilenames( renderableBeans );

            FileResolver resolver = new CacheFileResolver();
            for ( String signalFilename: signalFilenames ) {
                System.out.println("In load thread, STARTING load of volume " + new java.util.Date());

                Collection<String> labelFiles = getLabelsForSignalFile(renderableBeans, signalFilename);
                VolumeMaskBuilder volumeMaskBuilder = createMaskBuilder(
                        labelFiles, getRenderables(renderableBeans, signalFilename), resolver
                );

                mip3d.loadVolume( signalFilename, volumeMaskBuilder, resolver );
                // After first volume has been loaded, unset clear flag, so subsequent
                // ones are added.
                mip3d.setClearOnLoad(false);

                logger.info( "In load thread, ENDED load of volume." );
            }
        }

        // Strip any "show-loading" off the viewer.
        viewer.removeAll();

        // Add this last.  "show-loading" removes it.  This way, it is shown only
        // when it becomes un-busy.
        viewer.add( mip3d, BorderLayout.CENTER );

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
    private Collection<String> getSignalFilenames(Collection<RenderableBean> renderableBeans) {
        Collection<String> signalFileNames = new HashSet<String>();
        for ( RenderableBean bean: renderableBeans ) {
            if ( bean.getSignalFile() != null && bean.getSignalFile().trim().length() > 0 ) {
                signalFileNames.add( bean.getSignalFile() );
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

}

