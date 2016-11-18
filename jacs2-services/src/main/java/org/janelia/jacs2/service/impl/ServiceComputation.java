package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;

import java.util.concurrent.CompletionStage;

/**
 * ServiceComputation represents the actual computation for a specific input.
 */
public interface ServiceComputation {
    /**
     * This is the actual processing method which is performed on the enclosed service data.
     * @return a completion stage that could be chained with other computations
     */
    CompletionStage<ServiceInfo> processData();

    /**
     *
     * @param childInfo child service info
     * @return the computation for the child process.
     */
    ServiceComputation submitChildProcess(ServiceInfo childInfo);

    /**
     * @return this is the communication component of the current service through which data is entered in the computation.
     */
    ServiceSupplier getServiceSupplier();
}
