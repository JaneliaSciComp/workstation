package org.janelia.jacs2.service.impl;

import java.util.concurrent.ExecutorService;

public class ServiceComputationFactory {

    private final ExecutorService executor;

    public ServiceComputationFactory(ExecutorService executor) {
        this.executor = executor;
    }

    public <S, R> ServiceComputation<S, R> newComputation(S s) {
        return new FutureBasedServiceComputation<S, R>(s, executor);
    }

}
