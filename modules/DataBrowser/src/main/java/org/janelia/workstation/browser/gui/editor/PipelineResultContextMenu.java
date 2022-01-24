package org.janelia.workstation.browser.gui.editor;

import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.ContextualNodeActionUtils;

import java.awt.*;

/**
 * Right-click context menu for pipeline results and color depth images presented in the Sample Editor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PipelineResultContextMenu extends PopupContextMenu {

    PipelineResultContextMenu() {
        for (Component currentContextMenuItem : ContextualNodeActionUtils.getCurrentContextMenuItems()) {
            add(currentContextMenuItem);
        }
    }
}
