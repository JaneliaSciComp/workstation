package org.janelia.workstation.common.gui.listview;

/**
 * List viewers use this interface to communicate changes in their state up to their enclosing components.
 */
public interface ListViewerActionListener {

    /**
     * Called when something is hidden from view by a user filter.
     */
    void visibleObjectsChanged();

    /**
     * Called whenever the ViewerContext may have changed.
     */
    void viewerContextChanged();
}
