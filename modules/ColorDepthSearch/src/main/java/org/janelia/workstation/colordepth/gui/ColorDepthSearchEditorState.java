package org.janelia.workstation.colordepth;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.common.gui.editor.DomainObjectEditorState;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.openide.windows.TopComponent;

/**
 * Snapshot of the state of a color depth search.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchEditorState 
        implements DomainObjectEditorState<ColorDepthSearch,ColorDepthMatch,Reference> {

    @JsonIgnore
    private DomainListViewTopComponent topComponent;

    @JsonIgnore
    private DomainObjectNode<ColorDepthSearch> domainObjectNode;

    private ColorDepthSearch domainObject;
    private Integer page;
    private ListViewerState listViewerState;
    private Collection<Reference> selectedIds;
    private Reference selectedMask;
    private Integer searchResultIndex;
    
    @JsonCreator
    public ColorDepthSearchEditorState(
            @JsonProperty("domainObject") ColorDepthSearch domainObject, 
            @JsonProperty("selectedMask") Reference selectedMask, 
            @JsonProperty("searchResultIndex") Integer searchResultIndex, 
            @JsonProperty("page") Integer page, 
            @JsonProperty("listViewerState") ListViewerState listViewerState, 
            @JsonProperty("selectedIds") Collection<Reference> selectedIds) {
        this.domainObjectNode = null;
        this.domainObject = domainObject;
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
        this.selectedMask = selectedMask;
        this.searchResultIndex = searchResultIndex;
    }

    public ColorDepthSearchEditorState(DomainObjectNode<ColorDepthSearch> domainObjectNode,
                                       Reference selectedMask, Integer searchResultIndex, Integer page,
                                       ListViewerState listViewerState, Collection<Reference> selectedIds) {
        this.domainObjectNode = domainObjectNode;
        this.domainObject = domainObjectNode.getDomainObject();
        this.page = page;
        this.listViewerState = listViewerState;
        this.selectedIds = new ArrayList<>(selectedIds);
        this.selectedMask = selectedMask;
        this.searchResultIndex = searchResultIndex;
    }

    public <C extends TopComponent> C getTopComponent() {
        return (C)topComponent;
    }

    public <C extends TopComponent> void setTopComponent(C topComponent) {
        this.topComponent = (DomainListViewTopComponent)topComponent;
    }

    public ColorDepthSearch getDomainObject() {
        return domainObject;
    }

    public void setDomainObject(DomainObject domainObject) {
        this.domainObject = (ColorDepthSearch)domainObject;
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

    public DomainObjectNode<ColorDepthSearch> getDomainObjectNode() {
        return domainObjectNode;
    }

    public Reference getSelectedMask() {
        return selectedMask;
    }

    public Integer getSearchResultIndex() {
        return searchResultIndex;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ColorDepthSearchEditorState[");
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
        if (selectedMask != null) {
            builder.append("\n  selectedMask: ");
            builder.append(selectedMask);
        }
        if (searchResultIndex != null) {
            builder.append("\n  searchResultIndex: ");
            builder.append(searchResultIndex);
        }
        builder.append("\n]");
        return builder.toString();
    }

    
}
