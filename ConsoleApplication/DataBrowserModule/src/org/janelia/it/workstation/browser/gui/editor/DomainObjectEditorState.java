package org.janelia.it.workstation.browser.gui.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.model.DomainModelViewUtils;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.DomainObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Snapshot of the state of a list viewer for navigation purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class DomainObjectEditorState<P extends DomainObject, T, S> {

    @JsonIgnore
    private DomainListViewTopComponent topComponent;
    
    @JsonIgnore
    private AbstractDomainObjectNode<P> domainObjectNode;
    
    private P domainObject;
    private Integer page;
    private ListViewerState listViewerState;
    private Collection<S> selectedIds;

    @JsonCreator
    public DomainObjectEditorState(
            @JsonProperty("domainObject") P domainObject, 
            @JsonProperty("page") Integer page, 
            @JsonProperty("listViewerState") ListViewerState listViewerState, 
            @JsonProperty("selectedIds") Collection<S> selectedIds) {
        this.domainObjectNode = null;
        this.domainObject = domainObject;
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
    }
    
    public DomainObjectEditorState(AbstractDomainObjectNode<P> domainObjectNode, Integer page, ListViewerState listViewerState, Collection<S> selectedIds) {
        this.domainObjectNode = domainObjectNode;
        this.domainObject = domainObjectNode.getDomainObject();
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
    }

    public DomainListViewTopComponent getTopComponent() {
        return topComponent;
    }

    public void setTopComponent(DomainListViewTopComponent topComponent) {
        this.topComponent = topComponent;
    }

    public P getDomainObject() {
        return domainObject;
    }
    
    public void setDomainObject(DomainObject domainObject) {
        this.domainObject = (P)domainObject;
    }

    public Integer getPage() {
        return page;
    }

    public ListViewerState getListViewerState() {
        return listViewerState;
    }

    public Collection<S> getSelectedIds() {
        return selectedIds;
    }

    public AbstractDomainObjectNode<P> getDomainObjectNode() {
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

    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static String serialize(DomainObjectEditorState<?,?,?> state) throws Exception {
        return mapper.writeValueAsString(state);
    }

    public static DomainObjectEditorState<?,?,?> deserialize(String serializedState) throws Exception {
        return mapper.readValue(DomainModelViewUtils.convertModelPackages(serializedState), DomainObjectEditorState.class);
    }

}
