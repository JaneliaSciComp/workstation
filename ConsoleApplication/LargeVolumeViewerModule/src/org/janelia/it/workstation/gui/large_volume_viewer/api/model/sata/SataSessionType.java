package org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata;

public enum SataSessionType {
    
    AffinityLearning("Affinity Learning"),
    Reconstruction("Neuron Reconstruction");
    
    private String label;
    
    private SataSessionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
