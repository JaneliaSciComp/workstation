package org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata;

import java.util.List;

public class SataBranch {

    private String id;
    private List<String> nodeLocations;
    private List<String> parentIds;
    private List<String> childIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getNodeLocations() {
        return nodeLocations;
    }

    public void setNodeLocations(List<String> nodeLocations) {
        this.nodeLocations = nodeLocations;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    public List<String> getChildIds() {
        return childIds;
    }

    public void setChildIds(List<String> childIds) {
        this.childIds = childIds;
    }

}
