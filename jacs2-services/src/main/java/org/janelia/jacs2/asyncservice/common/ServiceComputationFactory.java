package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.cdi.qualifier.SuspendedTaskExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

@Singleton
public class ServiceComputationFactory {

    private final ServiceComputationQueue computationQueue;

    @Inject
    public ServiceComputationFactory(ServiceComputationQueue computationQueue) {
        this.computationQueue = computationQueue;
    }

    public <T> ServiceComputation<T> newComputation() {
        return new FutureBasedServiceComputation<T>(computationQueue);
    }

    /**
     * Create a completed computation.
     * @param result of the computation
     * @param <T>
     * @return
     */
    public <T> ServiceComputation<T> newCompletedComputation(T result) {
        return new FutureBasedServiceComputation<>(computationQueue, result);
    }

    /**
     * Create a completed failed computation.
     * @param exc exception thrown during the computation
     * @param <T> result type
     * @return a ServiceComputation that has a result of type <T></T>
     */
    public <T> ServiceComputation<T> newFailedComputation(Throwable exc) {
        return new FutureBasedServiceComputation<T>(computationQueue, exc);
    }
}
