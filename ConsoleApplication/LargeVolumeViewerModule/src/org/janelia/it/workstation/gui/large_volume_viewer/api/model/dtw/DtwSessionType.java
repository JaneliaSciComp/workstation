package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

public enum DtwSessionType {
    
    AffinityLearning("Affinity Learning"),
    Reconstruction("Neuron Reconstruction");
    
    private String label;
    
    private DtwSessionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
