package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Equivalence;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class AbstractBasicLifeCycleServiceProcessor<T> extends AbstractServiceProcessor<T> {

    protected final ServiceComputationFactory computationFactory;
    protected final JacsServiceDataPersistence jacsServiceDataPersistence;

    public AbstractBasicLifeCycleServiceProcessor(ServiceComputationFactory computationFactory,
                                                  JacsServiceDataPersistence jacsServiceDataPersistence,
                                                  String defaultWorkingDir,
                                                  Logger logger) {
        super(defaultWorkingDir, logger);
        this.computationFactory = computationFactory;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
    }

    @Override
    public ServiceComputation<T> process(JacsServiceData jacsServiceData) {
        JacsServiceData[] currentServiceDataHolder = new JacsServiceData[1]; // this will enclose the service data with the changes made by the prepareProcessing method in case there are any
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenApply(sd -> {
                    currentServiceDataHolder[0] = this.prepareProcessing(sd);
                    this.submitServiceDependencies(currentServiceDataHolder[0]);
                    return currentServiceDataHolder[0];
                })
                .thenApply(this::prepareProcessing)
                .thenSuspendUntil(() -> !suspendUntilAllDependenciesComplete(currentServiceDataHolder[0])) // suspend until all dependencies complete
                .thenCompose(this::processing)
                .thenSuspendUntil(() -> this.isResultReady(currentServiceDataHolder[0])) // wait until the result becomes available
                .thenApply(sd -> {
                    T r = this.getResultHandler().collectResult(sd);
                    this.getResultHandler().updateServiceDataResult(sd, r);
                    updateServiceData(sd);
                    return new JacsServiceResult<>(sd, r);
                })
                .thenApply(this::postProcessing)
                .whenComplete((r, exc) -> {
                    if (exc != null) {
                        fail(jacsServiceData, exc);
                    } else {
                        success(jacsServiceData);
                    }
                });
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return new DefaultServiceErrorChecker(logger);
    }

    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        setOutputPath(jacsServiceData);
        setErrorPath(jacsServiceData);
        return jacsServiceData;
    }

    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        return Collections.emptyList();
    }

    protected boolean isResultReady(JacsServiceData jacsServiceData) {
        if (getResultHandler().isResultReady(jacsServiceData)) {
            return true;
        }
        verifyTimeOut(jacsServiceData);
        return false;
    }

    /**
     * Suspend the service until the given service is done or until all dependencies are done
     * @param jacsServiceData service data
     * @return true if the service has been suspended
     */
    protected boolean suspendUntilAllDependenciesComplete(JacsServiceData jacsServiceData) {
        if (jacsServiceData.hasCompleted()) {
            return false;
        }
        if (areAllDependenciesDone(jacsServiceData)) {
            if (jacsServiceData.hasBeenSuspended()) {
                jacsServiceData.setState(JacsServiceState.RUNNING);
                updateServiceData(jacsServiceData);
            }
            return false;
        } else {
            if (!jacsServiceData.hasBeenSuspended()) {
                // if the service has not completed yet and it's not already suspended - update the state to suspended
                jacsServiceData.setState(JacsServiceState.SUSPENDED);
                updateServiceData(jacsServiceData);
            }
            return true;
        }
    }

    protected boolean areAllDependenciesDone(JacsServiceData jacsServiceData) {
        List<JacsServiceData> running = new ArrayList<>();
        List<JacsServiceData> failed = new ArrayList<>();
        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());
        if (jacsServiceDataHierarchy == null) {
            return true;
        }
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
            updateServiceData(jacsServiceData);
            logger.warn("Service {} canceled because of {}", jacsServiceData, failed);
            throw new ComputationException(jacsServiceData, "Service " + jacsServiceData.getId() + " canceled");
        }
        if (CollectionUtils.isEmpty(running)) {
            return true;
        }
        verifyTimeOut(jacsServiceData);
        return false;
    }

    protected void  verifyTimeOut(JacsServiceData jacsServiceData) {
        long timeSinceStart = System.currentTimeMillis() - jacsServiceData.getProcessStartTime().getTime();
        if (jacsServiceData.timeout() > 0 && timeSinceStart > jacsServiceData.timeout()) {
            jacsServiceData.setState(JacsServiceState.TIMEOUT);
            jacsServiceData.addEvent(JacsServiceEventTypes.TIMEOUT, String.format("Service timed out after %s ms", timeSinceStart));
            jacsServiceDataPersistence.update(jacsServiceData);
            logger.warn("Service {} timed out after {}ms", jacsServiceData, timeSinceStart);
            throw new ComputationException(jacsServiceData, "Service " + jacsServiceData.getId() + " timed out");
        }
    }

    protected abstract ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData);

    protected T postProcessing(JacsServiceResult<T> sr) {
        return sr.getResult();
    }

    protected void updateServiceData(JacsServiceData jacsServiceData) {
        if (jacsServiceData.hasId()) jacsServiceDataPersistence.update(jacsServiceData);
    }

    protected void success(JacsServiceData jacsServiceData) {
        if (jacsServiceData.hasCompletedSuccessfully()) {
            // nothing to do
            logger.info("Service {} has already been marked as successful", jacsServiceData);
            return;
        }
        logger.error("Processing successful {}:{}", jacsServiceData.getId(), jacsServiceData.getName());
        if (jacsServiceData.hasCompletedUnsuccessfully()) {
            logger.warn("Attempted to overwrite failed state with success for {}", jacsServiceData);
            return;
        }
        jacsServiceData.setState(JacsServiceState.SUCCESSFUL);
        jacsServiceData.addEvent(JacsServiceEventTypes.COMPLETED, "Completed successfully");
        updateServiceData(jacsServiceData);
    }

    protected void fail(JacsServiceData jacsServiceData, Throwable exc) {
        if (jacsServiceData.hasCompletedUnsuccessfully()) {
            // nothing to do
            logger.info("Service {} has already been marked as failed", jacsServiceData);
            return;
        }
        logger.error("Processing error executing {}:{}", jacsServiceData.getId(), jacsServiceData.getName(), exc);
        if (jacsServiceData.hasCompletedSuccessfully()) {
            logger.warn("Service {} has failed after has already been markes as successfully completed", jacsServiceData);
        }
        jacsServiceData.setState(JacsServiceState.ERROR);
        jacsServiceData.addEvent(JacsServiceEventTypes.FAILED, String.format("Failed: %s", exc.getMessage()));
        updateServiceData(jacsServiceData);
    }

    protected JacsServiceData submitDependencyIfNotPresent(JacsServiceData jacsServiceData, JacsServiceData dependency) {
        Optional<JacsServiceData> existingDependency = jacsServiceData.findSimilarDependency(dependency);
        if (existingDependency.isPresent()) {
            return existingDependency.get();
        } else {
            jacsServiceDataPersistence.saveHierarchy(dependency);
            return dependency;
        }
    }
}
