package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

    protected static final int N_RETRIES_FOR_RESULT = 60;
    protected static final long WAIT_BETWEEN_RETRIES_FOR_RESULT = 1000; // 1s

    protected final JacsServiceDispatcher jacsServiceDispatcher;
    protected final ServiceComputationFactory computationFactory;
    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final String defaultWorkingDir;
    protected final Logger logger;

    @Inject
    public AbstractServiceProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                    ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    Logger logger) {
        this.jacsServiceDispatcher = jacsServiceDispatcher;
        this.computationFactory= computationFactory;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.defaultWorkingDir = defaultWorkingDir;
        this.logger = logger;
    }

    @Override
    public ServiceComputation<T> process(JacsServiceData jacsServiceData) {
        return preProcessData(jacsServiceData)
                .thenCompose(preProcessingResults -> this.localProcessData(preProcessingResults, jacsServiceData))
                .thenCompose(r -> this.postProcessData(r, jacsServiceData));
    }

    protected ServiceComputation<?> preProcessData(JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

    protected abstract ServiceComputation<T> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected ServiceComputation<T> postProcessData(T processingResult, JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(processingResult);
    }

    /**
     * The method submits a service dependency {@code serviceDependency} whose result is required by {@code jacsServiceData} computation.
     *
     * @param jacsServiceData
     * @param childServiceData
     * @return
     */
    protected ServiceComputation<JacsServiceData> submitServiceDependency(JacsServiceData jacsServiceData, JacsServiceData serviceDependency) {
        serviceDependency.updateParentService(jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceDispatcher.submitServiceAsync(serviceDependency));
    }

    protected ServiceComputation<?> waitForCompletion(JacsServiceData jacsServiceData) {
        ServiceProcessor<?> serviceProcessor = jacsServiceDispatcher.getServiceProcessor(jacsServiceData);
        return computationFactory.<JacsServiceData>newComputation()
                .supply(() -> {
                    long startTime = System.currentTimeMillis();
                    for (; ; ) {
                        JacsServiceData sd = jacsServiceDataPersistence.findById(jacsServiceData.getId());
                        if (sd.hasCompletedSuccessfully()) {
                            return sd;
                        } else if (sd.hasCompletedUnsuccessfully()) {
                            logger.info("Service {} completed unsuccessfully", sd);
                            return sd;
                        } else {
                            try {
                                Thread.currentThread().sleep(1000);
                            } catch (InterruptedException e) {
                                logger.warn("Interrupt {}", jacsServiceData, e);
                                throw new ComputationException(jacsServiceData, e);
                            }
                        }
                        if (sd.timeout() > 0 && System.currentTimeMillis() - startTime > sd.timeout()) {
                            sd.setState(JacsServiceState.TIMEOUT);
                            return sd;
                        }
                    }
                })
                .thenApply(sd -> serviceProcessor.getResult(sd));
    }

    protected abstract boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected abstract T retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected T applyResult(T result, JacsServiceData jacsServiceData) {
        setResult(result, jacsServiceData);
        return result;
    }

    protected ServiceComputation<T> collectResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
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
        return Paths.get(workingDir, jacsServiceData.getName() + "_" + jacsServiceData.getId().toString()).toAbsolutePath();
    }

}
