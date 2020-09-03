package org.janelia.workstation.core.model;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;

public enum MappingType {
    Sample("Sample", Sample.class),
    LSM("LSM", LSMImage.class),
    NeuronFragment("Neuron Fragment", NeuronFragment.class),
    AlignedNeuronFragment("Aligned Neuron Fragment", NeuronFragment.class),
    UnalignedNeuronFragment("Unaligned Neuron Fragment", NeuronFragment.class);
    
    private String label;
    private Class<? extends DomainObject> clazz;
    
    MappingType(String label, Class<? extends DomainObject> clazz) {
        this.label = label;
        this.clazz = clazz;
    }
    
    public String getLabel() {
        return label;
    }

    public Class<? extends DomainObject> getClazz() {
        return clazz;
    }
}
