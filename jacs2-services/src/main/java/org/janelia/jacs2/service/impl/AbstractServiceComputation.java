package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public abstract class AbstractServiceComputation<R> implements ServiceComputation<R> {

    @Inject
    private Logger logger;
    @Inject
    private JacsServiceDispatcher serviceDispatcher;
    @Inject
    private JacsServiceDataPersistence jacsServiceDataPersistence;

    @Override
    public CompletionStage<JacsService<R>> preProcessData(JacsService<R> jacsService) {
        // the default operation is a noop
        return CompletableFuture.completedFuture(jacsService);
    }

    @Override
    public CompletionStage<JacsService<R>> isReadyToProcess(JacsService<R> jacsService) {
        return waitForChildServiceToComplete(jacsService);
    }

    @Override
    public CompletionStage<JacsService<R>> isDone(JacsService<R> jacsService) {
        return CompletableFuture.completedFuture(jacsService);
    }

    @Override
    public void postProcessData(JacsService<R> jacsService, Throwable exc) {
        // noop
    }

    protected CompletionStage<JacsService<R>> waitForChildServiceToComplete(JacsService<R> jacsService) {
        CompletableFuture<JacsService<R>> waitForChildrenToEndFuture = new CompletableFuture<>();
        List<JacsServiceData> uncompletedChildServices = jacsServiceDataPersistence.findServiceHierarchy(jacsService.getId());
        for (;;) {
            if (uncompletedChildServices.isEmpty()) {
                waitForChildrenToEndFuture.complete(jacsService);
                break;
            }
            List<JacsServiceData> firstPass =  uncompletedChildServices.stream()
                    .filter(ti -> !ti.getId().equals(jacsService.getId()))
                    .map(ti -> jacsServiceDataPersistence.findById(ti.getId()))
                    .filter(ti -> !ti.hasCompleted())
                    .collect(Collectors.toList());
            uncompletedChildServices = firstPass;
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Interrupt {}", jacsService, e);
                waitForChildrenToEndFuture.completeExceptionally(new ComputationException(jacsService, e));
                break;
            }
        }
        return waitForChildrenToEndFuture;
    }

}
