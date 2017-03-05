package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

    private static class SubComputationResults {
        final JacsServiceData jacsServiceData;
        final List<?> results;

        public SubComputationResults(JacsServiceData jacsServiceData, List<?> results) {
            this.jacsServiceData = jacsServiceData;
            this.results = results;
        }
    }

    protected static final int N_RETRIES_FOR_RESULT = 60;
    protected static final long WAIT_BETWEEN_RETRIES_FOR_RESULT = 1000; // 1s

    protected final JacsServiceEngine jacsServiceEngine;
    protected final ServiceComputationFactory computationFactory;
    protected final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final String defaultWorkingDir;
    protected final Logger logger;

    public AbstractServiceProcessor(JacsServiceEngine jacsServiceEngine,
                                    ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    String defaultWorkingDir,
                                    Logger logger) {
        this.jacsServiceEngine = jacsServiceEngine;
        this.computationFactory= computationFactory;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.defaultWorkingDir = defaultWorkingDir;
        this.logger = logger;
    }

    @Override
    public ServiceComputation<T> process(ServiceExecutionContext executionContext, ServiceArg... args) {
        JacsServiceData serviceData = submit(executionContext, JacsServiceState.QUEUED, args);
        return process(serviceData);
    }

    private JacsServiceData submit(ServiceExecutionContext executionContext, JacsServiceState serviceState, ServiceArg... args) {
        ServiceMetaData smd = getMetadata();
        JacsServiceDataBuilder jacsServiceDataBuilder =
                new JacsServiceDataBuilder(executionContext.getParentServiceData())
                        .setName(smd.getServiceName())
                        .setProcessingLocation(executionContext.getProcessingLocation())
                        .setState(serviceState)
                        .addArg(Stream.of(args).flatMap(arg -> Stream.of(arg.toStringArray())).toArray(String[]::new));
        executionContext.getWaitFor().forEach(sd -> jacsServiceDataBuilder.addDependency(sd));
        if (executionContext.getParentServiceData() != null) {
            executionContext.getParentServiceData().getDependeciesIds().forEach(did -> jacsServiceDataBuilder.addDependencyId(did));
        }
        JacsServiceData jacsServiceData = jacsServiceDataBuilder.build();
        return jacsServiceEngine.submitSingleService(jacsServiceData);
    }

    @Override
    public ServiceComputation<T> process(JacsServiceData jacsServiceData) {
        jacsServiceData.setProcessStartTime(new Date());
        jacsServiceDataPersistence.save(jacsServiceData);
        return prepareProcessing(jacsServiceData)
                .thenCombineAll(this.invokeServiceDependencies(jacsServiceData), (sd, depResults) -> new SubComputationResults(sd, depResults))
                .thenSuspendUntil(() -> {
                    // suspend until all dependencies complete
                    if (checkForDependenciesCompletion(jacsServiceData)) {
                        if (jacsServiceData.hasBeenSuspended()) {
                            jacsServiceData.setState(JacsServiceState.RUNNING);
                            jacsServiceDataPersistence.save(jacsServiceData);
                        }
                        return true;
                    } else {
                        suspend(jacsServiceData);
                        return false;
                    }
                })
                .thenApply(scr -> this.processing(scr.jacsServiceData, scr.results))
                .thenApply(r -> this.postProcessing(jacsServiceData, r))
                ;
    }

    protected abstract T getResult(JacsServiceData jacsServiceData);

    protected abstract ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData);

    protected abstract List<ServiceComputation<?>> invokeServiceDependencies(JacsServiceData jacsServiceData);

    protected abstract T processing(JacsServiceData jacsServiceData, List<?> dependencyResults);

    protected abstract T postProcessing(JacsServiceData jacsServiceData, T result);

    protected boolean checkForDependenciesCompletion(JacsServiceData jacsServiceData) {
        List<JacsServiceData> running = new ArrayList<>();
        List<JacsServiceData> failed = new ArrayList<>();
        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());
        jacsServiceDataHierarchy.serviceHierarchyStream()
                .filter(sd -> !sd.getId().equals(jacsServiceData.getId()))
                .forEach(sd -> {
                    if (!sd.hasCompleted()) {
                        running.add(sd);
                    } else if (sd.hasCompletedUnsuccessfully()) {
                        failed.add(sd);
                    }
                });
        if (CollectionUtils.isNotEmpty(failed)) {
            jacsServiceData.setState(JacsServiceState.CANCELED);
            jacsServiceData.addEvent(JacsServiceEventTypes.CANCELED,
                    String.format("Canceled because one or more service dependencies finished unsuccessfully: %s", failed));
            jacsServiceDataPersistence.update(jacsServiceData);
            logger.warn("Service {} canceled because of {}", jacsServiceData, failed);
            throw new ComputationException(jacsServiceData, "Service " + jacsServiceData.getId() + " canceled");
        }
        if (running.isEmpty()) {
            return true;
        }
        long timeSinceStart = System.currentTimeMillis() - jacsServiceData.getProcessStartTime().getTime();
        if (jacsServiceData.timeout() > 0 && timeSinceStart > jacsServiceData.timeout()) {
            jacsServiceData.setState(JacsServiceState.TIMEOUT);
            jacsServiceData.addEvent(JacsServiceEventTypes.TIMEOUT, String.format("Service timed out after %s ms", timeSinceStart));
            jacsServiceDataPersistence.update(jacsServiceData);
            logger.warn("Service {} timed out after {}ms", jacsServiceData, timeSinceStart);
            throw new ComputationException(jacsServiceData, "Service " + jacsServiceData.getId() + " timed out");
        }
        return false;
    }

    private void suspend(JacsServiceData jacsServiceData) {
        if (!jacsServiceData.hasCompleted()) {
            if (!jacsServiceData.hasBeenSuspended()) {
                updateStateToSuspended(jacsServiceData);
            }
        }
    }

    private void updateStateToSuspended(JacsServiceData jacsServiceData) {
        jacsServiceData.setState(JacsServiceState.SUSPENDED);
        jacsServiceDataPersistence.save(jacsServiceData);
        jacsServiceData.addEvent(JacsServiceEventTypes.SUSPEND, String.format("Suspended: %s", jacsServiceData.getName()));
    }

}
