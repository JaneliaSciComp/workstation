package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.jacs.model.entity.Entity;

public interface BrowserModelListener extends GenericModelListener {

    /** The axis of the master editor
    */
    void browserMasterEditorEntityChanged(Entity masterEditorEntity);

//    /**
//     * Notification that the SubView's fixed range has changed.  This is the
//     * whole range that all SubViews will work with.  This will not change often.
//     */
//    void browserSubViewFixedRangeChanged(Range subViewFixedRange);
//
//    /**
//     * Notification that the SubView's visible range has changed.  This is the
//     * visible range of the SubView.  It denotes where the user is looking within
//     * the fixed range and will probably change often.
//     */
//    void browserSubViewVisibleRangeChanged(Range subViewVisibleRange);
//
    /**
     * Notification that the current system seleciton has changed
     */
    void browserCurrentSelectionChanged(Entity newSelection);

//    /**
//     * The selected range on the masterAxis, if any
//     */
//    public void browserMasterEditorSelectedRangeChanged(Range masterEditorSelectedRange);

    /*
     * Notification that the observed browser is going away...
     */
    public void browserClosing();
}
