package org.janelia.it.workstation.browser.gui.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;

/**
 * Snapshot of the state of a list viewer for navigation purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditorState<T extends DomainObject> {

    private final AbstractDomainObjectNode<T> domainObjectNode;
    private final T domainObject;
    private final Integer page;
    private final ListViewerState listViewerState;
    private final Collection<Reference> selectedIds;

    private DomainListViewTopComponent topComponent;

    public DomainObjectEditorState(T domainObject, Integer page, ListViewerState listViewerState, Collection<Reference> selectedIds) {
        this.domainObjectNode = null;
        this.domainObject = domainObject;
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
    }
    
    public DomainObjectEditorState(AbstractDomainObjectNode<T> domainObjectNode, Integer page, ListViewerState listViewerState, Collection<Reference> selectedIds) {
        this.domainObjectNode = domainObjectNode;
        this.domainObject = domainObjectNode.getDomainObject();
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
    }

    public AbstractDomainObjectNode<T> getDomainObjectNode() {
        return domainObjectNode;
    }

    public T getDomainObject() {
        return domainObject;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DomainObjectEditorState[");
        if (topComponent != null) {
            builder.append("\n  topComponent: ");
            builder.append(topComponent.getClass().getSimpleName());
        }
        if (domainObjectNode != null) {
            builder.append("\n  domainObject: ");
            builder.append(domainObject.getName());
            builder.append(" (");
            builder.append(domainObject);
            builder.append(")");
        }
        if (page != null) {
            builder.append("\n  page: ");
            builder.append(page);
        }
        if (listViewerState != null) {
            builder.append("\n  listViewerState: ");
            builder.append(listViewerState);
        }
        if (selectedIds != null) {
            builder.append("\n  selectedIds: ");
            builder.append(selectedIds);
        }
        builder.append("\n]");
        return builder.toString();
    }
    
    

}
