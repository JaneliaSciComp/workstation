package org.janelia.it.workstation.gui.browser.gui.listview;

import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.DomainObjectIconGridViewer;
import org.janelia.it.workstation.gui.browser.gui.listview.table.DomainObjectTableViewer;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ListViewerType {

    IconViewer("Icon View", DomainObjectIconGridViewer.class),
    TableViewer("Table View", DomainObjectTableViewer.class);

    private final String name;
    private final Class<? extends AnnotatedDomainObjectListViewer> viewerClass;

    ListViewerType(String name, Class<? extends AnnotatedDomainObjectListViewer> viewerClass) {
        this.name = name;
        this.viewerClass = viewerClass;
    }

    public String getName() {
        return name;
    }

    public Class<? extends AnnotatedDomainObjectListViewer> getViewerClass() {
        return viewerClass;
    }
}
