package org.janelia.it.workstation.browser.gui.listview;

import org.janelia.it.workstation.browser.gui.colordepth.ColorDepthResultIconGridViewer;
import org.janelia.it.workstation.browser.gui.listview.icongrid.DomainObjectIconGridViewer;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;

/**
 * Enumeration of list viewer classes. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ListViewerType {

    IconViewer("Image View", DomainObjectIconGridViewer.class),
    TableViewer("Table View", DomainObjectTableViewer.class),
    ColorDepthResultViewer("Image View", ColorDepthResultIconGridViewer.class);

    private final String name;
    private final Class<? extends ListViewer<?,?>> viewerClass;

    ListViewerType(String name, Class<? extends ListViewer<?,?>> viewerClass) {
        this.name = name;
        this.viewerClass = viewerClass;
    }

    public String getName() {
        return name;
    }

    public Class<? extends ListViewer<?,?>> getViewerClass() {
        return viewerClass;
    }
}
