package org.janelia.it.workstation.gui.framework.session_mgr;

import org.janelia.it.jacs.model.entity.Entity;

public class BrowserModelListenerAdapter implements BrowserModelListener {

    public void browserMasterEditorEntityChanged(Entity masterEditorEntity) {
    }
//
//  public void browserMasterEditorSelectedRangeChanged(Range masterEditorSelectedRange) {}
//
//  public void browserSubViewFixedRangeChanged(Range subViewFixedRange) {}
//
//  public void browserSubViewVisibleRangeChanged(Range subViewVisibleRange){}

    public void browserCurrentSelectionChanged(Entity newSelection) {
    }

    public void browserClosing() {
    }

    public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
    }
}