package org.janelia.it.workstation.browser.gui.listview;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface ListViewerClassProvider {

    String getName();

    Class<? extends ListViewer<?,?>> getViewerClass();
}
