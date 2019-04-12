package org.janelia.workstation.core.model;

public enum MappingType {
    Sample("Sample"),
    LSM("LSM"),
    NeuronFragment("Neuron Fragment"),
    AlignedNeuronFragment("Aligned Neuron Fragment"),
    UnalignedNeuronFragment("Unaligned Neuron Fragment");
    
    private String label;
    
    private MappingType(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
