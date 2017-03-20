package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AbstractBasicLifeCycleServiceProcessor<T> extends AbstractServiceProcessor<T> {

    protected static final int N_RETRIES_FOR_RESULT = 60;
    protected static final long WAIT_BETWEEN_RETRIES_FOR_RESULT = 1000; // 1s

    protected final JacsServiceDataPersistence jacsServiceDataPersistence;

    public AbstractBasicLifeCycleServiceProcessor(JacsServiceEngine jacsServiceEngine,
                                                  ServiceComputationFactory computationFactory,
                                                  JacsServiceDataPersistence jacsServiceDataPersistence,
                                                  String defaultWorkingDir,
                                                  Logger logger) {
        super(jacsServiceEngine, computationFactory, defaultWorkingDir, logger);
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
    }

    @Override
    public ServiceComputation<T> process(JacsServiceData jacsServiceData) {
        jacsServiceData.setProcessStartTime(new Date());
        jacsServiceDataPersistence.save(jacsServiceData);
        return prepareProcessing(jacsServiceData)
                .thenApply(this::submitServiceDependencies)
                .thenSuspendUntil(() -> this.checkSuspendCondition(jacsServiceData))
                .thenCompose(scr -> this.processing(jacsServiceData))
                .thenApply(r -> {
                    success(jacsServiceData, Optional.ofNullable(r));
                    return r;
                })
                .thenCompose(r -> this.postProcessing(jacsServiceData, r))
                .exceptionally(exc -> {
                    fail(jacsServiceData, exc);
                    return null;
                })
                ;
    }

    protected abstract T getResult(JacsServiceData jacsServiceData);

    protected abstract void setResult(T result, JacsServiceData jacsServiceData);

    protected abstract ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData);

    protected abstract List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData);

    protected abstract ServiceComputation<T> processing(JacsServiceData jacsServiceData);

    protected ServiceComputation<T> postProcessing(JacsServiceData jacsServiceData, T result) {
        return createComputation(result);
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

    protected abstract boolean isResultAvailable(JacsServiceData jacsServiceData);

    protected abstract T retrieveResult(JacsServiceData jacsServiceData);

    protected T waitForResult(JacsServiceData jacsServiceData) {
        for (int i = 0; i < N_RETRIES_FOR_RESULT; i ++) {
            if (isResultAvailable(jacsServiceData)) {
                logger.info("Found result on try # {}", i + 1);
                return retrieveResult(jacsServiceData);
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

    protected boolean checkSuspendCondition(JacsServiceData jacsServiceData) {
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

    protected void execute(Consumer<JacsServiceData> execFn, JacsServiceData jacsServiceData) {
        try {
            execFn.accept(jacsServiceData);
            success(jacsServiceData, Optional.ofNullable(retrieveResult(jacsServiceData)));
        } catch (Exception e) {
            fail(jacsServiceData, e);
        }
    }

    protected void success(JacsServiceData jacsServiceData, Optional<T> result) {
        JacsServiceData updatedSD = jacsServiceDataPersistence.findById(jacsServiceData.getId());
        if (!updatedSD.hasCompletedUnsuccessfully()) {
            if (result.isPresent()) {
                this.setResult(result.get(), updatedSD);
            }
            updatedSD.setState(JacsServiceState.SUCCESSFUL);
            updatedSD.addEvent(JacsServiceEventTypes.COMPLETED, "Completed successfully");
            jacsServiceDataPersistence.save(updatedSD);
        } else {
            logger.warn("Attempted to overwrite failed state with success for {}", jacsServiceData);
        }
    }

    protected void fail(JacsServiceData jacsServiceData, Throwable exc) {
        logger.error("Processing error executing {}:{}", jacsServiceData.getId(), jacsServiceData.getName(), exc);
        JacsServiceData updatedSD = jacsServiceDataPersistence.findById(jacsServiceData.getId());
        if (!updatedSD.hasCompleted()) {
            if (exc instanceof SuspendedException) {
                if (!updatedSD.hasCompleted() && !updatedSD.hasBeenSuspended()) {
                    updateStateToSuspended(updatedSD);
                }
            } else if (!updatedSD.hasCompletedUnsuccessfully()) {
                updatedSD.addEvent(JacsServiceEventTypes.FAILED, String.format("Failed: %s", exc.getMessage()));
                updatedSD.setState(JacsServiceState.ERROR);
                jacsServiceDataPersistence.save(updatedSD);
            }
        }
    }
}
