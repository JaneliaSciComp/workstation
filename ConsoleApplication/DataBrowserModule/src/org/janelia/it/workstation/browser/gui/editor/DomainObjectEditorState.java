package org.janelia.it.workstation.browser.gui.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Snapshot of the state of a list viewer for navigation purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectEditorState<T extends DomainObject> {

    @JsonIgnore
    private DomainListViewTopComponent topComponent;
    
    @JsonIgnore
    private AbstractDomainObjectNode<T> domainObjectNode;
    
    private T domainObject;
    private Integer page;
    private ListViewerState listViewerState;
    private Collection<Reference> selectedIds;

    @JsonCreator
    public DomainObjectEditorState(
            @JsonProperty("domainObject") T domainObject, 
            @JsonProperty("page") Integer page, 
            @JsonProperty("listViewerState") ListViewerState listViewerState, 
            @JsonProperty("selectedIds") Collection<Reference> selectedIds) {
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

    public DomainListViewTopComponent getTopComponent() {
        return topComponent;
    }

    public void setTopComponent(DomainListViewTopComponent topComponent) {
        this.topComponent = topComponent;
    }

    public T getDomainObject() {
        return domainObject;
    }
    
    public void setDomainObject(DomainObject domainObject) {
        this.domainObject = (T)domainObject;
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

    public AbstractDomainObjectNode<T> getDomainObjectNode() {
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
    
    public static String serialize(DomainObjectEditorState<?> descriptor) throws Exception {
        return mapper.writeValueAsString(descriptor);
    }

    public static DomainObjectEditorState<?> deserialize(String artifactDescriptorString) throws Exception {
        return mapper.readValue(artifactDescriptorString, DomainObjectEditorState.class);
    }

}
