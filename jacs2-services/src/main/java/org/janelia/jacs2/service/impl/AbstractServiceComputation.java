package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsServiceDispatcher serviceDispatcher;
    @Inject
    private JacsServiceDataPersistence jacsServiceDataPersistence;

    @Override
    public CompletionStage<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        // the default operation is a noop
        return CompletableFuture.completedFuture(jacsServiceData);
    }

    @Override
    public CompletionStage<JacsServiceData> isReady(JacsServiceData jacsServiceData) {
        return waitForChildServiceToComplete(jacsServiceData);
    }

    @Override
    public CompletionStage<JacsServiceData> isDone(JacsServiceData jacsServiceData) {
        CompletableFuture<JacsServiceData> checkIfDoneFuture = new CompletableFuture<>();
        checkIfDoneFuture.complete(jacsServiceData);
        return checkIfDoneFuture;
    }

    @Override
    public void postProcessData(JacsServiceData jacsServiceData, Throwable exc) {
        // noop
    }

    @Override
    public ServiceComputation submitChildServiceAsync(JacsServiceData childServiceData, JacsServiceData parentService) {
        logger.info("Create child service {}", childServiceData);
        childServiceData.setOwner(parentService.getOwner());
        JacsServiceData serviceData = serviceDispatcher.submitServiceAsync(childServiceData, Optional.of(parentService));
        return serviceDispatcher.getServiceComputation(serviceData);
    }

    protected CompletionStage<JacsServiceData> waitForChildServiceToComplete(JacsServiceData jacsServiceData) {
        CompletableFuture<JacsServiceData> waitForChildrenToEndFuture = new CompletableFuture<>();
        List<JacsServiceData> uncompletedChildServices = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());
        for (;;) {
            if (uncompletedChildServices.isEmpty()) {
                waitForChildrenToEndFuture.complete(jacsServiceData);
                break;
            }
            List<JacsServiceData> firstPass =  uncompletedChildServices.stream()
                    .map(ti -> jacsServiceDataPersistence.findById(ti.getId()))
                    .filter(ti -> !ti.hasCompleted())
                    .collect(Collectors.toList());
            uncompletedChildServices = firstPass;
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Interrup {}", jacsServiceData, e);
                waitForChildrenToEndFuture.completeExceptionally(e);
                break;
            }
        }
        return waitForChildrenToEndFuture;
    }

}
