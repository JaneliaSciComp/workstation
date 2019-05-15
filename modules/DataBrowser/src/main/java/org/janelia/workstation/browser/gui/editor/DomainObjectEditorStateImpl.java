package org.janelia.workstation.browser.gui.editor;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.openide.windows.TopComponent;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditorStateImpl<P extends DomainObject, T> implements DomainObjectEditorState<P, T, Reference> {

    @JsonIgnore
    private DomainListViewTopComponent topComponent;

    @JsonIgnore
    private DomainObjectNode<P> domainObjectNode;

    private P domainObject;
    private Integer page;
    private ListViewerState listViewerState;
    private Collection<Reference> selectedIds;

    @JsonCreator
    public DomainObjectEditorStateImpl(
            @JsonProperty("domainObject") P domainObject,
            @JsonProperty("page") Integer page,
            @JsonProperty("listViewerState") ListViewerState listViewerState,
            @JsonProperty("selectedIds") Collection<Reference> selectedIds) {
        this.domainObjectNode = null;
        this.domainObject = domainObject;
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
    }

    public DomainObjectEditorStateImpl(DomainObjectNode<P> domainObjectNode, Integer page, ListViewerState listViewerState, Collection<Reference> selectedIds) {
        this.domainObjectNode = domainObjectNode;
        this.domainObject = domainObjectNode.getDomainObject();
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
    }

    public <C extends TopComponent> C getTopComponent() {
        return (C) topComponent;
    }

    public <C extends TopComponent> void setTopComponent(C topComponent) {
        this.topComponent = (DomainListViewTopComponent) topComponent;
    }

    public P getDomainObject() {
        return domainObject;
    }

    public void setDomainObject(DomainObject domainObject) {
        this.domainObject = (P) domainObject;
    }

    public Integer getPage() {
        return page;
    }

    public ListViewerState getListViewerState() {
        return listViewerState;
    }

    public Collection<Reference> getSelectedIds() {
        return selectedIds;
    }

    public DomainObjectNode<P> getDomainObjectNode() {
        return domainObjectNode;
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
