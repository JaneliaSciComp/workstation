package org.janelia.workstation.controller;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores the changes cascading from a neuron filter update
 */
public class NeuronUpdates {
    private Set<Long> addedNeurons = new HashSet<>();
    private Set<Long> deletedNeurons = new HashSet<>();

    public Set<Long> getAddedNeurons() {
        return addedNeurons;
    }
    public void setAddedNeurons(Set<Long> addedNeurons) {
        this.addedNeurons = addedNeurons;
    }
    public Set<Long> getDeletedNeurons() {
        return deletedNeurons;
    }
    public void setDeletedNeurons(Set<Long> deletedNeurons) {
        this.deletedNeurons = deletedNeurons;
    }
}
