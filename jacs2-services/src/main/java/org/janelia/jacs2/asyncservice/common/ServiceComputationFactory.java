package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.cdi.qualifier.SuspendedTaskExecutor;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServiceComputationFactory {

    private final ServiceComputationQueue computationQueue;
    private final Logger logger;

    @Inject
    public ServiceComputationFactory(ServiceComputationQueue computationQueue, Logger logger) {
        this.computationQueue = computationQueue;
        this.logger = logger;
    }

    public <T> ServiceComputation<T> newComputation() {
        return new FutureBasedServiceComputation<T>(computationQueue, logger);
    }

    /**
     * Create a completed computation.
     * @param result of the computation
     * @param <T>
     * @return
     */
    public <T> ServiceComputation<T> newCompletedComputation(T result) {
        return new FutureBasedServiceComputation<>(computationQueue, logger, result);
    }

    /**
     * Create a completed failed computation.
     * @param exc exception thrown during the computation
     * @param <T> result type
     * @return a ServiceComputation that has a result of type <T></T>
     */
    public <T> ServiceComputation<T> newFailedComputation(Throwable exc) {
        return new FutureBasedServiceComputation<T>(computationQueue, logger, exc);
    }
}
