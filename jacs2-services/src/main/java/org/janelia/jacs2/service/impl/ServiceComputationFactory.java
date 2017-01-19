package org.janelia.jacs2.service.impl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

@Singleton
public class ServiceComputationFactory {

    private final ExecutorService executor;

    @Inject
    public ServiceComputationFactory(ExecutorService executor) {
        this.executor = executor;
    }

    public <T> ServiceComputation<T> newComputation() {
        return new FutureBasedServiceComputation<>(executor);
    }

    /**
     * Create a completed computation.
     * @param result of the computation
     * @param <T>
     * @return
     */
    public <T> ServiceComputation<T> newCompletedComputation(T result) {
        return new FutureBasedServiceComputation<>(executor, result);
    }

    /**
     * Create a completed failed computation.
     * @param exc exception thrown during the computation
     * @param <T>
     * @return
     */
    public <T> ServiceComputation<T> newFailedComputation(Throwable exc) {
        return new FutureBasedServiceComputation<T>(executor, exc);
    }
}
