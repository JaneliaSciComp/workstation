package org.janelia.it.workstation.browser.gui.colordepth;

import org.janelia.it.workstation.browser.gui.listview.ListViewer;
import org.janelia.it.workstation.browser.gui.listview.ListViewerClassProvider;

/**
 * Enumeration of list viewer classes. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ColorDepthListViewerType implements ListViewerClassProvider {

    ColorDepthResultImageViewer("Image View", ColorDepthResultIconGridViewer.class),
    ColorDepthResultTableViewer("Table View", ColorDepthResultTableViewer.class);

    private final String name;
    private final Class<? extends ListViewer<?,?>> viewerClass;

    ColorDepthListViewerType(String name, Class<? extends ListViewer<?,?>> viewerClass) {
        this.name = name;
        this.viewerClass = viewerClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<? extends ListViewer<?,?>> getViewerClass() {
        return viewerClass;
    }
}
