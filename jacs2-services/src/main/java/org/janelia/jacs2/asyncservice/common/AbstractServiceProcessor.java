package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

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
    public JacsServiceData create(ServiceExecutionContext executionContext, ServiceArg... args) {
        return create(executionContext, JacsServiceState.QUEUED, args);
    }

    @Override
    public ServiceComputation<JacsServiceData> invoke(ServiceExecutionContext executionContext, ServiceArg... args) {
        return createComputation(create(executionContext, JacsServiceState.RUNNING, args))
                .thenCompose(sd -> this.process(sd));
    }

    @Override
    public ServiceComputation<JacsServiceData> invokeAsync(ServiceExecutionContext executionContext, ServiceArg... args) {
        return createComputation(create(executionContext, JacsServiceState.QUEUED, args));
    }

    private JacsServiceData create(ServiceExecutionContext executionContext, JacsServiceState serviceState, ServiceArg... args) {
        ServiceMetaData smd = getMetadata();
        JacsServiceDataBuilder jacsServiceDataBuilder =
                new JacsServiceDataBuilder(executionContext.getParentServiceData())
                        .setName(smd.getServiceName())
                        .setProcessingLocation(executionContext.getProcessingLocation())
                        .setState(serviceState)
                        .addArg(Stream.of(args).flatMap(arg -> Stream.of(arg.toStringArray())).toArray(String[]::new));
        executionContext.getWaitFor().forEach(sd -> jacsServiceDataBuilder.addDependency(sd));
        JacsServiceData jacsServiceData = jacsServiceDataBuilder.build();
        return jacsServiceEngine.submitSingleService(jacsServiceData);
    }

    @Override
    public ServiceComputation<JacsServiceData> process(JacsServiceData jacsServiceData) {
        jacsServiceData.setProcessStartTime(new Date());
        return preProcessData(jacsServiceData)
                .thenApply(sd -> {
                    if (!sd.hasBeenSuspended()) {
                        this.submitAllDependencies(sd);
                    }
                    return sd;
                })
                .thenCompose(sd -> this.waitForDependencies(sd))
                .thenCompose(sd -> this.processData(sd))
                .thenCompose(sd -> this.postProcessData(sd))
                .thenApply(sd -> {
                    JacsServiceData updatedSD = jacsServiceDataPersistence.findById(sd.getId());
                    retrieveResultWhenReady(updatedSD);
                    updatedSD.setState(JacsServiceState.SUCCESSFUL);
                    updatedSD.addEvent(JacsServiceEventTypes.COMPLETED, "Completed successfully");
                    jacsServiceDataPersistence.save(updatedSD);
                    return updatedSD;
                })
                .exceptionally(exc -> {
                    logger.error("Processing error executing {}", jacsServiceData.getId(), exc);
                    JacsServiceData updatedSD = jacsServiceDataPersistence.findById(jacsServiceData.getId());
                    if (exc instanceof SuspendedException) {
                        if (!updatedSD.hasCompleted() && !updatedSD.hasBeenSuspended()) {
                            updateStateToSuspended(updatedSD);
                        }
                    } else if (!updatedSD.hasCompletedUnsuccessfully()) {
                        updatedSD.addEvent(JacsServiceEventTypes.FAILED, String.format("Failed: %s", exc.getMessage()));
                        updatedSD.setState(JacsServiceState.ERROR);
                        jacsServiceDataPersistence.save(updatedSD);
                    }
                    return updatedSD;
                })
                ;
    }

    protected ServiceComputation<JacsServiceData> waitForDependencies(JacsServiceData jacsServiceData) {
        return computationFactory.<JacsServiceData>newComputation()
                .supply(() -> {
                    for (;;) {
                        if (checkForDependenciesCompletion(jacsServiceData)) {
                            jacsServiceData.setState(JacsServiceState.RUNNING);
                            jacsServiceDataPersistence.save(jacsServiceData);
                            return jacsServiceData;
                        }
                        suspend(jacsServiceData);
                        try {
                            Thread.sleep(10000L);
                        } catch (InterruptedException e) {
                            logger.warn("Interrupt checking for dependencies thread", e);
                        }
                    }
                });
    }

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

    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    protected abstract ServiceComputation<JacsServiceData> processData(JacsServiceData jacsServiceData);

    protected ServiceComputation<JacsServiceData> postProcessData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    protected abstract List<JacsServiceData> submitAllDependencies(JacsServiceData jacsServiceData);

    protected ServiceComputation<JacsServiceData> createFailure(JacsServiceData jacsServiceData, Throwable exc) {
        return computationFactory.newFailedComputation(exc);
    }

    protected ServiceComputation<JacsServiceData> createComputation(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    protected boolean checkForCompletion(JacsServiceData jacsServiceData) {
        JacsServiceData sd = jacsServiceDataPersistence.findById(jacsServiceData.getId());
        if (sd.hasCompletedSuccessfully()) {
            return true;
        } else if (sd.hasCompletedUnsuccessfully()) {
            logger.warn("Service {} completed unsuccessfully", sd);
            throw new ComputationException(sd, "Service " + sd + " did not complete successfully");
        }
        List<JacsServiceData> childServices = jacsServiceDataPersistence.findChildServices(jacsServiceData.getId());
        Optional<JacsServiceData> failedChildService = childServices.stream().filter(cs -> cs.hasCompletedUnsuccessfully()).findFirst();
        if (failedChildService.isPresent()) {
            sd.setState(JacsServiceState.CANCELED);
            sd.addEvent(JacsServiceEventTypes.CANCELED, String.format("Canceled because child service %d - %s %s -  finished unsuccessfully",
                    failedChildService.get().getId(), failedChildService.get().getName(), failedChildService.get().getArgs()));
            jacsServiceDataPersistence.update(sd);
            logger.warn("Service {} canceled because of {}", sd, failedChildService.get());
            throw new ComputationException(sd, "Service " + sd + " canceled");
        }
        long timeSinceStart = System.currentTimeMillis() - jacsServiceData.getProcessStartTime().getTime();
        if (sd.timeout() > 0 && timeSinceStart > sd.timeout()) {
            logger.warn("Service {} timed out after {}ms", sd, timeSinceStart);
            sd.setState(JacsServiceState.TIMEOUT);
            sd.addEvent(JacsServiceEventTypes.CANCELED, "Canceled because timeout");
            jacsServiceDataPersistence.update(sd);
            throw new ComputationException(sd, "Service " + sd + " timed out");
        }
        return false;
    }

    protected abstract boolean isResultAvailable(JacsServiceData jacsServiceData);

    protected abstract T retrieveResult(JacsServiceData jacsServiceData);

    protected T retrieveResultWhenReady(JacsServiceData jacsServiceData) {
        for (int i = 0; i < N_RETRIES_FOR_RESULT; i ++) {
            if (isResultAvailable(jacsServiceData)) {
                logger.info("Found result on try # {}", i + 1);
                T result = retrieveResult(jacsServiceData);
                setResult(result, jacsServiceData);
                return result;
            }
            logger.info("Result not found on try # {}", i + 1);
            try {
                Thread.sleep(WAIT_BETWEEN_RETRIES_FOR_RESULT);
            } catch (InterruptedException e) {
                throw new ComputationException(jacsServiceData, e);
            }
            long timeSinceStart = System.currentTimeMillis() - jacsServiceData.getProcessStartTime().getTime();
            if (jacsServiceData.timeout() > 0 &&  timeSinceStart > jacsServiceData.timeout()) {
                logger.warn("Service {} timed out after {}ms while collecting the results", jacsServiceData, timeSinceStart);
                jacsServiceData.setState(JacsServiceState.TIMEOUT);
                jacsServiceData.addEvent(JacsServiceEventTypes.CANCELED, "Canceled because of retrieve results timeout");
                jacsServiceDataPersistence.update(jacsServiceData);
                throw new ComputationException(jacsServiceData, "Retrieve results for service " + jacsServiceData + " timed out");
            }
        }
        jacsServiceData.setState(JacsServiceState.TIMEOUT);
        jacsServiceData.addEvent(JacsServiceEventTypes.CANCELED, "Canceled because results could not be retrieved after " + N_RETRIES_FOR_RESULT + " retries");
        jacsServiceDataPersistence.update(jacsServiceData);
        throw new ComputationException(jacsServiceData, "Results could not be retrieved after " + N_RETRIES_FOR_RESULT + " retries");
    }

    protected Path getWorkingDirectory(JacsServiceData jacsServiceData) {
        String workingDir;
        if (StringUtils.isNotBlank(jacsServiceData.getWorkspace())) {
            workingDir = jacsServiceData.getWorkspace();
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            workingDir = defaultWorkingDir;
        } else {
            workingDir = System.getProperty("java.io.tmpdir");
        }
        return getServicePath(workingDir, jacsServiceData);
    }

    protected Path getServicePath(String baseDir, JacsServiceData jacsServiceData, String... more) {
        List<String> pathElems = new ImmutableList.Builder<String>()
                .add(jacsServiceData.getName() + "_" + jacsServiceData.getId().toString())
                .addAll(Arrays.asList(more))
                .build();
        return Paths.get(baseDir, pathElems.toArray(new String[0])).toAbsolutePath();
    }
}
