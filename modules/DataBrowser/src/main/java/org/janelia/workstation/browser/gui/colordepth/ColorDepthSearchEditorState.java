package org.janelia.workstation.browser.gui.colordepth;

import java.util.Collection;

import org.janelia.workstation.browser.gui.editor.DomainObjectEditorStateImpl;
import org.janelia.workstation.core.nodes.DomainObjectNode;
import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Snapshot of the state of a color depth search.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchEditorState 
        extends DomainObjectEditorStateImpl<ColorDepthSearch, ColorDepthMatch, String> {

    private Reference selectedMask;
    private Integer searchResultIndex;
    
    @JsonCreator
    public ColorDepthSearchEditorState(
            @JsonProperty("domainObject") ColorDepthSearch domainObject, 
            @JsonProperty("selectedMask") Reference selectedMask, 
            @JsonProperty("searchResultIndex") Integer searchResultIndex, 
            @JsonProperty("page") Integer page, 
            @JsonProperty("listViewerState") ListViewerState listViewerState, 
            @JsonProperty("selectedIds") Collection<String> selectedIds) {
        super(domainObject, page, listViewerState, selectedIds);
        this.selectedMask = selectedMask;
        this.searchResultIndex = searchResultIndex;
    }

    public ColorDepthSearchEditorState(DomainObjectNode<ColorDepthSearch> domainObjectNode,
                                       Reference selectedMask, Integer searchResultIndex, Integer page,
                                       ListViewerState listViewerState, Collection<String> selectedIds) {
        super(domainObjectNode, page, listViewerState, selectedIds);
        this.selectedMask = selectedMask;
        this.searchResultIndex = searchResultIndex;
    }

    public Reference getSelectedMask() {
        return selectedMask;
    }

    public Integer getSearchResultIndex() {
        return searchResultIndex;
    }
    
}
