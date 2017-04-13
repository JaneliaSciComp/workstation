package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

/**
 * Service processor parameterized on the result type
 * @param <T> result type
 */
public interface ServiceProcessor<T> {
    /**
     * @return service metadata
     */
    ServiceMetaData getMetadata();

    /**
     * Create service data based on the current execution context and the provided arguments.
     * @param executionContext current execution context.
     * @param args service arguments.
     * @return service data.
     */
    JacsServiceData createServiceData(ServiceExecutionContext executionContext, ServiceArg... args);

    /**
     * @return service result handler
     */
    ServiceResultHandler<T> getResultHandler();

    /**
     * @return service error checker
     */
    ServiceErrorChecker getErrorChecker();

    ServiceComputation<T> process(JacsServiceData jacsServiceData);

    /**
     * Default process mechanism given the execution context and the service arguments.
     * @param executionContext current execution context.
     * @param args service arguments.
     * @return service information.
     */
    default ServiceComputation<T> process(ServiceExecutionContext executionContext, ServiceArg... args) {
        return process(createServiceData(executionContext, args));
    }
}
