package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.AlignmentBoardControlsDialog;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSource;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanMultiFileLoader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;

import java.util.Arrays;

/**
 * Setup a surface collector for minimal inputs.
 *
 * Created by fosterl on 3/27/14.
 */
public class VoxelSurfaceCollectorFactory {
    private FileResolver resolver;
    private AlignmentBoardSettings alignmentBoardSettings;

    public VoxelSurfaceCollectorFactory( FileResolver resolver, AlignmentBoardSettings alignmentBoardSettings ) {
        this.resolver = resolver;
        this.alignmentBoardSettings = alignmentBoardSettings;
    }

    public VoxelSurfaceCollector getSurfaceCollector( final MaskChanRenderableData renderableData ) throws Exception {
        RenderableBean renderableBean = renderableData.getBean();
        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();

        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setShowChannelData( false );
        settings.setGammaFactor( AlignmentBoardSettings.DEFAULT_GAMMA );
        settings.setChosenDownSampleRate(AlignmentBoardControlsDialog.UNSELECTED_DOWNSAMPLE_RATE);

        VoxelSurfaceCollector surfaceCollector = new VoxelSurfaceCollector();
        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList(surfaceCollector) );

        MaskChanStreamSource streamSource = new MaskChanStreamSource(
                renderableData, resolver, alignmentBoardSettings.isShowChannelData()
        );

        loader.read(renderableBean, streamSource);
        loader.close();
        return surfaceCollector;

    }
}
