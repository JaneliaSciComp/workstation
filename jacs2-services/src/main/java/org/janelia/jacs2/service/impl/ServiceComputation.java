package org.janelia.jacs2.service.impl;

import java.util.concurrent.CompletionStage;

/**
 * ServiceComputation represents the actual computation for a specific input that generates a result of type <R>.
 *
 * @param <R> computation result type
 */
public interface ServiceComputation<R> {
    /**
     * Pre-processing stage.
     *
     * @param jacsService service to be pre-processed.
     * @return a promise corresponding to the pre-processing stage.
     */
    CompletionStage<JacsService<R>> preProcessData(JacsService<R> jacsService);

    /**
     * Check if the service is ready for processing.
     * @param jacsService service to be checked if it can be processed.
     * @return the corresponding completion stage for this check.
     */
    CompletionStage<JacsService<R>> isReadyToProcess(JacsService<R> jacsService);

    /**
     * This is the actual processing method which is performed on the enclosed service data.
     * @param jacsService service to be processed.
     * @return a completion stage that could be chained with other computations
     */
    CompletionStage<JacsService<R>> processData(JacsService<R> jacsService);

    /**
     * Check if the service processing is done.
     * @param jacsService service to be checked if it is done.
     * @return the corresponding completion stage for this check.
     */
    CompletionStage<JacsService<R>> isDone(JacsService<R> jacsService);

    /**
     * Post-processing.
     * @param jacsService service data.
     * @param exc exception that might have been raised during the computation.
     */
    void postProcessData(JacsService<R> jacsService, Throwable exc);
}
