package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.apache.juli.JdkLoggerFormatter;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AlignmentBoardViewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Mip3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.ColorMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.CacheFileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.AlignmentBoardDataBuilder;
import org.janelia.it.jacs.model.entity.Entity;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/13/13
 * Time: 3:21 PM
 *
 * Takes on all tasks for pushing data into the Alignment Board Viewer, given its base entity.
 */
public class ABLoadWorker extends SimpleWorker {

    private Entity alignmentBoard;
    private Mip3d mip3d;
    private AlignmentBoardViewer viewer;
    private ColorMappingI colorMapping;

    public ABLoadWorker(
            AlignmentBoardViewer viewer, Entity alignmentBoard, Mip3d mip3d, ColorMappingI colorMapping
    ) {
        this.alignmentBoard = alignmentBoard;
        this.mip3d = mip3d;
        this.viewer = viewer;
        this.colorMapping = colorMapping;
    }

    @Override
    protected void doStuff() throws Exception {

        mip3d.setClearOnLoad( true );

        System.out.println("In load thread, before getting bean list " + new java.util.Date());
        Collection<RenderableBean> renderableBeans =
                new AlignmentBoardDataBuilder()
                    .setAlignmentBoard( alignmentBoard )
                        .getRenderableBeanList();

        if ( renderableBeans == null  ||  renderableBeans.size() == 0 ) {
            return;
        }

        System.out.println("In load thread, after getting bean list " + new java.util.Date());
        mip3d.setMaskColorMappings( colorMapping.getMapping( renderableBeans ) );

        Collection<String> signalFilenames = getSignalFilenames( renderableBeans );

        FileResolver resolver = new CacheFileResolver();
        for ( String signalFilename: signalFilenames ) {
            System.out.println("In load thread, STARTING load of volume " + new java.util.Date());

            Collection<String> maskFilenamesForSignal = getMaskFilenames( renderableBeans, signalFilename );
            VolumeMaskBuilder volumeMaskBuilder = createMaskBuilder(
                    maskFilenamesForSignal, getRenderables(renderableBeans, signalFilename), resolver
            );

            mip3d.loadVolume( signalFilename, volumeMaskBuilder, resolver );
            // After first volume has been loaded, unset clear flag, so subsequent
            // ones are added.
            mip3d.setClearOnLoad(false);

            System.out.println("In load thread, ENDED load of volume " + new java.util.Date());
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
            if ( bean.getSignalFile().equals( signalFilename ) ) {
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
            signalFileNames.add( bean.getSignalFile() );
        }
        return signalFileNames;
    }

    /**
     * Extract the mask file names from the renderable beans.
     *
     * @param signalFilename which signal to find masks against.
     * @return all mask file names from beans which refer to the signal file name given.
     */
    private Collection<String> getMaskFilenames( Collection<RenderableBean> renderableBeans, String signalFilename ) {
        Collection<String> maskFileNames = new HashSet<String>();
        for ( RenderableBean bean: renderableBeans ) {
            if ( bean.getSignalFile().equals( signalFilename ) ) {
                maskFileNames.add( bean.getLabelFile() );
            }
        }
        return maskFileNames;
    }

    /**
     * Accumulates all data for masking, from the set of files provided, preparing them for
     * injection into th evolume being loaded.
     *
     * @param maskFiles list of all mask files to use against the signal volumes.
     */
    private VolumeMaskBuilder createMaskBuilder(
            Collection<String> maskFiles, Collection<RenderableBean> renderables, FileResolver resolver
    ) {

        VolumeMaskBuilder volumeMaskBuilder = null;
        // Build the masking texture info.
        if (maskFiles != null  &&  maskFiles.size() > 0) {
            VolumeMaskBuilder builder = new VolumeMaskBuilder();
            builder.setRenderables(renderables);
            for ( String maskFile: maskFiles ) {
                VolumeLoader volumeLoader = new VolumeLoader( resolver );
                volumeLoader.loadVolume(maskFile);
                volumeLoader.populateVolumeAcceptor(builder);
            }
            volumeMaskBuilder = builder;
        }

        return volumeMaskBuilder;
    }

}

