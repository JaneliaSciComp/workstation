package org.janelia.it.workstation.browser.gui.listview;

import org.janelia.it.workstation.browser.gui.listview.icongrid.DomainObjectIconGridViewer;
import org.janelia.it.workstation.browser.gui.listview.table.DomainObjectTableViewer;

/**
 * Enumeration of list viewer classes. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ListViewerType implements ListViewerClassProvider {

    IconViewer("Image View", DomainObjectIconGridViewer.class),
    TableViewer("Table View", DomainObjectTableViewer.class);

    private final String name;
    private final Class<? extends ListViewer<?,?>> viewerClass;

    ListViewerType(String name, Class<? extends ListViewer<?,?>> viewerClass) {
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
