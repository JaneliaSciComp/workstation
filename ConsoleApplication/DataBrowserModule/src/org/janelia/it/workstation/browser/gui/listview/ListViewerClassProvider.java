package org.janelia.it.workstation.browser.gui.listview;

public interface ListViewerClassProvider {

    String getName();

    Class<? extends ListViewer<?,?>> getViewerClass();
}
