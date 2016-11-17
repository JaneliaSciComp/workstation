package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;

import java.util.concurrent.CompletionStage;

/**
 * ServiceComputation represents the actual computation for a specific input.
 */
public interface ServiceComputation {
    ServiceInfo getComputationInfo();
    void setComputationInfo(ServiceInfo si);
    CompletionStage<ServiceComputation> processData();
}
