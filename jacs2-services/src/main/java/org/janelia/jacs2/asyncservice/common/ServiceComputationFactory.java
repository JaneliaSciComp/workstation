package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.cdi.qualifier.SuspendedTaskExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

@Singleton
public class ServiceComputationFactory {

    private final ExecutorService executor;
    private final ExecutorService suspendedExecutor;

    @Inject
    public ServiceComputationFactory(ExecutorService executor, @SuspendedTaskExecutor ExecutorService suspendedExecutor) {
        this.executor = executor;
        this.suspendedExecutor = suspendedExecutor;
    }

    public <T> ServiceComputation<T> newComputation() {
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor);
    }

    /**
     * Create a completed computation.
     * @param result of the computation
     * @param <T>
     * @return
     */
    public <T> ServiceComputation<T> newCompletedComputation(T result) {
        return new FutureBasedServiceComputation<>(executor, suspendedExecutor, result);
    }

    /**
     * Create a completed failed computation.
     * @param exc exception thrown during the computation
     * @param <T>
     * @return
     */
    public <T> ServiceComputation<T> newFailedComputation(Throwable exc) {
        return new FutureBasedServiceComputation<T>(executor, suspendedExecutor, exc);
    }
}
