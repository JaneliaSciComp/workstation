package org.janelia.workstation.browser.gui.editor;

import java.awt.Component;

import org.janelia.model.domain.sample.FileGroup;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.actions.ContextualNodeActionUtils;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context pop up menu for entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileGroupContextMenu extends PopupContextMenu {

    private static final Logger log = LoggerFactory.getLogger(FileGroupContextMenu.class);

    private ChildSelectionModel<FileGroup,String> selectionModel;

    public void addMenuItems() {
        for (Component currentContextMenuItem : ContextualNodeActionUtils.getCurrentContextMenuItems()) {
            add(currentContextMenuItem);
        }
    }
}
