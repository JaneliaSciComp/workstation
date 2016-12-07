package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.concurrent.CompletionStage;

/**
 * ServiceComputation represents the actual computation for a specific input.
 */
public interface ServiceComputation {
    /**
     * Pre-processing stage.
     *
     * @return a promise corresponding to the pre-processing stage.
     */
    CompletionStage<JacsServiceData> preProcessData(JacsServiceData jacsServiceData);

    /**
     * This is the actual processing method which is performed on the enclosed service data.
     * @return a completion stage that could be chained with other computations
     */
    CompletionStage<JacsServiceData> processData(JacsServiceData jacsServiceData);

    /**
     * Check if the service is ready for processing.
     * @param jacsServiceData service to be checked if it can be processed.
     * @return the corresponding completion stage for this check.
     */
    CompletionStage<JacsServiceData> isReady(JacsServiceData jacsServiceData);

    /**
     * Check if the service processing is done.
     * @param jacsServiceData service to be checked if it is done.
     * @return the corresponding completion stage for this check.
     */
    CompletionStage<JacsServiceData> isDone(JacsServiceData jacsServiceData);

    /**
     * Post-processing.
     */
    void postProcessData(JacsServiceData jacsServiceData, Throwable exc);

    /**
     * Submit a child service.
     * @param childServiceData child service info
     * @param parentService parent service info
     * @return the computation for the child process.
     */
    ServiceComputation submitChildServiceAsync(JacsServiceData childServiceData, JacsServiceData parentService);
}
