package org.janelia.it.workstation.gui.browser.components.viewer;

import org.janelia.it.workstation.gui.browser.components.icongrid.DomainObjectIconGridViewer;
import org.janelia.it.workstation.gui.browser.components.table.DomainObjectTableViewer;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ViewerType {

    IconViewer("Icon View", DomainObjectIconGridViewer.class),
    TableViewer("Table View", DomainObjectTableViewer.class),
    HybridViewer("Hybrid View", null);

    private final String name;
    private final Class<? extends AnnotatedDomainObjectListViewer> viewerClass;

    ViewerType(String name, Class<? extends AnnotatedDomainObjectListViewer> viewerClass) {
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
