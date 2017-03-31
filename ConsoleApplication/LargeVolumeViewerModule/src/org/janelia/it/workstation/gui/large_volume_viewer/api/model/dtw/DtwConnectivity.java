package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

import java.util.List;

public class DtwConnectivity {

    private List<List<String>> connections;
    private Double probability;

    public List<List<String>> getConnections() {
        return connections;
    }

    public void setConnections(List<List<String>> connections) {
        this.connections = connections;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

}
