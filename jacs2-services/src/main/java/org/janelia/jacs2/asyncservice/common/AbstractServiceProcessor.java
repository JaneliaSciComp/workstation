package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import java.util.stream.Stream;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

    protected final JacsServiceEngine jacsServiceEngine;
    protected final ServiceComputationFactory computationFactory;
    protected final Logger logger;

    public AbstractServiceProcessor(JacsServiceEngine jacsServiceEngine,
                                    ServiceComputationFactory computationFactory,
                                    Logger logger) {
        this.jacsServiceEngine = jacsServiceEngine;
        this.computationFactory= computationFactory;
        this.logger = logger;
    }

    @Override
    public ServiceComputation<T> process(ServiceExecutionContext executionContext, ServiceArg... args) {
        executionContext.setServiceState(JacsServiceState.SUBMITTED);
        JacsServiceData serviceData = submit(executionContext, args);
        return process(serviceData);
    }

    @Override
    public JacsServiceData submit(ServiceExecutionContext executionContext, ServiceArg... args) {
        JacsServiceData jacsServiceData = createJacsServiceData(executionContext, args);
        return jacsServiceEngine.submitSingleService(jacsServiceData);
    }

    protected JacsServiceData createJacsServiceData(ServiceExecutionContext executionContext, ServiceArg... args) {
        ServiceMetaData smd = getMetadata();
        JacsServiceDataBuilder jacsServiceDataBuilder =
                new JacsServiceDataBuilder(executionContext.getParentServiceData())
                        .setName(smd.getServiceName())
                        .setProcessingLocation(executionContext.getProcessingLocation())
                        .addArg(Stream.of(args).flatMap(arg -> Stream.of(arg.toStringArray())).toArray(String[]::new));
        if (executionContext.getServiceState() != null) {
            jacsServiceDataBuilder.setState(executionContext.getServiceState());
        }
        executionContext.getWaitFor().forEach(sd -> jacsServiceDataBuilder.addDependency(sd));
        if (executionContext.getParentServiceData() != null) {
            executionContext.getParentServiceData().getDependeciesIds().forEach(did -> jacsServiceDataBuilder.addDependencyId(did));
        }
        return jacsServiceDataBuilder.build();
    }

    protected <U> ServiceComputation<U> createComputation(U data) {
        return computationFactory.newCompletedComputation(data);
    }

    protected <U> ServiceComputation<U> createFailure(Throwable exc) {
        return computationFactory.newFailedComputation(exc);
    }

}
