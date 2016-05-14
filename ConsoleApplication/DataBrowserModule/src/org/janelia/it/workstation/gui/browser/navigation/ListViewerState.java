package org.janelia.it.workstation.gui.browser.navigation;

import org.janelia.it.workstation.gui.browser.gui.listview.ListViewerType;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ListViewerState {

    private ListViewerType type;

    public ListViewerState(ListViewerType type) {
        this.type = type;
    }

    public ListViewerType getType() {
        return type;
    }
}
