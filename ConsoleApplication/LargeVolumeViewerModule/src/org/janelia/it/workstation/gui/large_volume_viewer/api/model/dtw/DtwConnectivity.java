package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

import java.util.List;

/**
 * Connectivity DTO for communicating with the Directed Tracing Workflow Service.
 * 
 * Represents a possible set of branch connections, with an associated probability.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DtwConnectivity {

    private List<String> connections;
    private Double probability;

    public List<String> getConnections() {
        return connections;
    }

    public void setConnections(List<String> connections) {
        this.connections = connections;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

}
