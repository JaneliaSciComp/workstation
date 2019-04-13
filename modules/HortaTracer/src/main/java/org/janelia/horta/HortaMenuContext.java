package org.janelia.horta;

import java.awt.Point;
import javax.swing.JPopupMenu;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.blocks.BlockTileSource;
import org.janelia.horta.render.NeuronMPRenderer;
import org.janelia.scenewindow.SceneWindow;

/**
 *
 * @author brunsc
 */
class HortaMenuContext {
    public final JPopupMenu topMenu;
    public final Point popupScreenPoint;
    public final Vector3 mouseXyz; // coordinate at mouse location
    public final Vector3 focusXyz; // coordinate at center of screen
    public final NeuronMPRenderer renderer;
    public final SceneWindow sceneWindow;

    HortaMenuContext(
            JPopupMenu menu, 
            Point popupScreenPoint,
            Vector3 mouseXyz, 
            Vector3 focusXyz,
            NeuronMPRenderer renderer,
            SceneWindow sceneWindow
    ) {
        this.topMenu = menu;
        this.popupScreenPoint = popupScreenPoint;
        this.mouseXyz = mouseXyz;
        this.focusXyz = focusXyz;
        this.renderer = renderer;
        this.sceneWindow = sceneWindow;
    }
    
}
