package org.janelia.it.workstation.gui.browser.gui.editor;

import java.util.Collection;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.gui.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;

/**
 * Snapshot of the state of a list viewer for navigation purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditorState {

    private final DomainObjectNode domainObjectNode;
    private final Integer page;
    private final ListViewerState listViewerState;
    private final Collection<Reference> selectedIds;

    private DomainListViewTopComponent topComponent;

    public DomainObjectEditorState(DomainObjectNode domainObjectNode, Integer page, ListViewerState listViewerState, Collection<Reference> selectedIds) {
        this.domainObjectNode = domainObjectNode;
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = selectedIds;
    }

    public DomainObjectNode getDomainObjectNode() {
        return domainObjectNode;
    }

    public Integer getPage() {
        return page;
    }

    public DomainListViewTopComponent getTopComponent() {
        return topComponent;
    }

    public ListViewerState getListViewerState() {
        return listViewerState;
    }

    public Collection<Reference> getSelectedIds() {
        return selectedIds;
    }

    public void setTopComponent(DomainListViewTopComponent topComponent) {
        this.topComponent = topComponent;
    }

}
