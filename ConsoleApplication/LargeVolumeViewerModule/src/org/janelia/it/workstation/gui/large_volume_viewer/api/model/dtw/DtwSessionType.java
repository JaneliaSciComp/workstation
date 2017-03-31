package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

/**
 * Session type DTO for communicating with the Directed Tracing Workflow Service.
 * 
 * Represents the type of tracing session being executed by the user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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
