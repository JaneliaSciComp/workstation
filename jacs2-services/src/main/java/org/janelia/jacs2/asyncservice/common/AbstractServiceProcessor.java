package org.janelia.jacs2.asyncservice.common;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    public ServiceComputation<T> invoke(ServiceExecutionContext executionContext, String... args) {
        return invokeService(executionContext, JacsServiceState.RUNNING, args)
                .thenCompose(sd -> this.process(sd));
    }

    @Override
    public ServiceComputation<JacsServiceData> invokeAsync(ServiceExecutionContext executionContext, String... args) {
        return invokeService(executionContext, JacsServiceState.QUEUED, args);
    }

    private ServiceComputation<JacsServiceData> invokeService(ServiceExecutionContext executionContext, JacsServiceState serviceState, String... args) {
        ServiceMetaData smd = getMetadata();
        JacsServiceData jacsServiceData =
                new JacsServiceDataBuilder(executionContext.getParentServiceData())
                        .setName(smd.getServiceName())
                        .setProcessingLocation(executionContext.getProcessingLocation())
                        .setState(serviceState)
                        .addArg(args)
                        .build();
        jacsServiceEngine.submitSingleService(jacsServiceData);
        return createServiceComputation(jacsServiceData);
    }

    @Override
    public ServiceComputation<T> process(JacsServiceData jacsServiceData) {
        return preProcessData(jacsServiceData)
                .thenCompose(preProcessingResults -> this.localProcessData(preProcessingResults, jacsServiceData))
                .thenCompose(r -> this.postProcessData(r, jacsServiceData))
                .whenComplete((r, exc) -> {
                    if (exc == null) {
                        logger.info("Successfully completed {}", jacsServiceData);
                        jacsServiceData.addEvent(JacsServiceEventTypes.COMPLETED, "Completed successfully");
                        jacsServiceData.setState(JacsServiceState.SUCCESSFUL);
                    } else {
                        // if the service data state has already been marked as cancelled or error leave it as is
                        if (!jacsServiceData.hasCompletedUnsuccessfully()) {
                            logger.error("Error executing {}", jacsServiceData, exc);
                            jacsServiceData.addEvent(JacsServiceEventTypes.FAILED, String.format("Failed: %s", exc.getMessage()));
                            jacsServiceData.setState(JacsServiceState.ERROR);
                        }
                    }
                    jacsServiceDataPersistence.save(jacsServiceData);
                });
    }

    protected ServiceComputation<?> preProcessData(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    protected abstract ServiceComputation<T> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected ServiceComputation<T> postProcessData(T processingResult, JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(processingResult);
    }

    protected ServiceComputation<JacsServiceData> createServiceComputation(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    protected ServiceComputation<JacsServiceData> waitForCompletion(JacsServiceData jacsServiceData) {
        return computationFactory.<JacsServiceData>newComputation()
                .supply(() -> {
                    long startTime = System.currentTimeMillis();
                    for (; ; ) {
                        JacsServiceData sd = jacsServiceDataPersistence.findById(jacsServiceData.getId());
                        if (sd.hasCompletedSuccessfully()) {
                            return sd;
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
                        try {
                            Thread.currentThread().sleep(1000);
                        } catch (InterruptedException e) {
                            logger.warn("Interrupt {}", jacsServiceData, e);
                            throw new ComputationException(jacsServiceData, e);
                        }
                        long timeSinceStart = System.currentTimeMillis() - startTime;
                        if (sd.timeout() > 0 && timeSinceStart > sd.timeout()) {
                            logger.warn("Service {} timed out after {}ms", sd, timeSinceStart);
                            sd.setState(JacsServiceState.TIMEOUT);
                            throw new ComputationException(sd, "Service " + sd + " timed out");
                        }
                    }
                });
    }

    protected abstract boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected abstract T retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected T applyResult(T result, JacsServiceData jacsServiceData) {
        setResult(result, jacsServiceData);
        return result;
    }

    protected ServiceComputation<T> collectResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < N_RETRIES_FOR_RESULT; i ++) {
            if (isResultAvailable(preProcessingResult, jacsServiceData)) {
                logger.info("Found result on try # {}", i + 1);
                T result = retrieveResult(preProcessingResult, jacsServiceData);
                setResult(result, jacsServiceData);
                return computationFactory.newCompletedComputation(result);
            }
            logger.info("Result not found on try # {}", i + 1);
            try {
                Thread.sleep(WAIT_BETWEEN_RETRIES_FOR_RESULT);
            } catch (InterruptedException e) {
                throw new ComputationException(jacsServiceData, e);
            }
            long timeSinceStart = System.currentTimeMillis() - startTime;
            if (jacsServiceData.timeout() > 0 &&  timeSinceStart > jacsServiceData.timeout()) {
                logger.warn("Service {} timed out after {}ms while collecting the results", jacsServiceData, timeSinceStart);
                jacsServiceData.setState(JacsServiceState.TIMEOUT);
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Service " + jacsServiceData + " timed out"));
            }
        }
        return computationFactory.newFailedComputation(new ComputationException(jacsServiceData,
                "Could not retrieve result for " + jacsServiceData.toString()));
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
