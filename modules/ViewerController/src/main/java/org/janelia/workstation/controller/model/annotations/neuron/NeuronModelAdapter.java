package org.janelia.workstation.controller.model.annotations.neuron;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

interface NeuronModelAdapter {
    Stream<TmNeuronMetadata> loadNeurons(TmWorkspace workspace);
    CompletableFuture<TmNeuronMetadata> asyncCreateNeuron(TmNeuronMetadata neuron) throws Exception;
    public void asyncSaveNeuron(TmNeuronMetadata neuron, Map<String, String> extraArgs) throws Exception;
    public void asyncDeleteNeuron(TmNeuronMetadata neuron) throws Exception;
    CompletableFuture<Boolean> requestOwnership(TmNeuronMetadata neuron) throws Exception;
    void requestAssignment(TmNeuronMetadata neuron, String targetUser) throws Exception;
}
