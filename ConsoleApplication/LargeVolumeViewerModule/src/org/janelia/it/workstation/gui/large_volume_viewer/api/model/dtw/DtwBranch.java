package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

import java.util.List;

/**
 * Branch DTO for communicating with the Directed Tracing Workflow Service.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DtwBranch {

    private String id;
    private List<String> nodes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }
}
