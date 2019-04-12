package org.janelia.workstation.browser.gui.editor;

import java.util.Collection;

import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.common.nodes.DomainObjectNode;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.workspace.GroupedFolder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Snapshot of the state of a group folder editor.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupedFolderEditorState 
        extends DomainObjectEditorStateImpl<GroupedFolder, DomainObject, Reference> {

    private Reference selectedGroup;
    
    @JsonCreator
    public GroupedFolderEditorState(
            @JsonProperty("domainObject") GroupedFolder domainObject, 
            @JsonProperty("selectedGroup") Reference selectedGroup, 
            @JsonProperty("page") Integer page, 
            @JsonProperty("listViewerState") ListViewerState listViewerState, 
            @JsonProperty("selectedIds") Collection<Reference> selectedIds) {
        super(domainObject, page, listViewerState, selectedIds);
        this.selectedGroup = selectedGroup;
    }

    public GroupedFolderEditorState(
            DomainObjectNode<GroupedFolder> domainObjectNode,
            Reference selectedGroup, 
            Integer page, 
            ListViewerState listViewerState, 
            Collection<Reference> selectedIds) {
        super(domainObjectNode, page, listViewerState, selectedIds);
        this.selectedGroup = selectedGroup;
    }

    public Reference getSelectedGroup() {
        return selectedGroup;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GroupedFolderEditorState[\n");
        if (selectedGroup != null) {
            builder.append("  selectedGroup=");
            builder.append(selectedGroup);
        }
        builder.append("\n]\n");
        builder.append(super.toString());
        return builder.toString();
    }
    
}
