package org.janelia.workstation.controller.model.annotations.neuron;

@FunctionalInterface
public interface ThrowingLambda {
    void accept() throws Exception;
}
