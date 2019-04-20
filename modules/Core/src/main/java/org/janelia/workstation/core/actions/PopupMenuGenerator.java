package org.janelia.workstation.core.actions;

import javax.swing.JMenuItem;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface PopupMenuGenerator {

    /**
     * Generate a popup menu item.
     * @return popup menu item
     */
    JMenuItem getPopupPresenter();
}
