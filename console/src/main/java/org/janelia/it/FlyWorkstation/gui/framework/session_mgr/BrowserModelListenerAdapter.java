package org.janelia.it.FlyWorkstation.gui.framework.session_mgr;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Title:        Genome Browser Client
 * Description:  This project is for JBuilder 4.0
 * @author Peter Davies
 */

public class BrowserModelListenerAdapter implements BrowserModelListener {

  public void browserMasterEditorEntityChanged(Entity masterEditorEntity) {}
//
//  public void browserMasterEditorSelectedRangeChanged(Range masterEditorSelectedRange) {}
//
//  public void browserSubViewFixedRangeChanged(Range subViewFixedRange) {}
//
//  public void browserSubViewVisibleRangeChanged(Range subViewVisibleRange){}

  public void browserCurrentSelectionChanged(Entity newSelection) {}

  public void browserClosing() {}

  public void modelPropertyChanged(Object key, Object oldValue, Object newValue){}
}