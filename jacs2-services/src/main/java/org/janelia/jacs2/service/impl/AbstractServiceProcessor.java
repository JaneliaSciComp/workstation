package org.janelia.jacs2.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractServiceProcessor<T> implements ServiceProcessor<T> {

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

    protected abstract ServiceComputation<T> localProcessData(Object preprocessingResults, JacsServiceData jacsServiceData);

    protected ServiceComputation<T> postProcessData(T processingResult, JacsServiceData jacsServiceData) {
        return computationFactory.newCompletedComputation(processingResult);
    }

    /**
     * The method submits a child service {@code childServiceData} in the context of the {@code jacsServiceData}
     *
     * @param jacsServiceData
     * @param childServiceData
     * @return
     */
    protected ServiceComputation<JacsServiceData> submitChildService(JacsServiceData jacsServiceData, JacsServiceData childServiceData) {
        childServiceData.updateParentService(jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceDispatcher.submitServiceAsync(childServiceData));
    }

    /**
     * The method submits the parent service {@code parentServiceData} in the context of the {@code jacsServiceData}
     *
     * @param jacsServiceData
     * @param childServiceData
     * @return
     */
    protected ServiceComputation<JacsServiceData> submitParentService(JacsServiceData jacsServiceData, JacsServiceData parentServiceData) {
        jacsServiceData.updateParentService(parentServiceData);
        return computationFactory.newCompletedComputation(jacsServiceDispatcher.submitServiceAsync(parentServiceData));
    }

    protected ServiceComputation<?> waitForCompletion(JacsServiceData jacsServiceData) {
        ServiceProcessor<?> serviceProcessor = jacsServiceDispatcher.getServiceProcessor(jacsServiceData);
        return computationFactory.<JacsServiceData>newComputation()
                .supply(() -> {
                    for (;;) {
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
                    }
                })
                .thenApply(sd -> serviceProcessor.getResult(sd));
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
