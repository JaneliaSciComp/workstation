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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractServiceComputation<R> implements ServiceComputation<R> {

    private static class ChildServiceResults {
        private List<JacsServiceData> successfullyCompleted = new ArrayList<>();
        private List<JacsServiceData> unsuccessfullyCompleted = new ArrayList<>();
        private List<JacsServiceData> uncompleted = new ArrayList<>();
    }

    @PropertyValue(name = "service.DefaultWorkingDir")
    @Inject
    private String defaultWorkingDir;
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
            ChildServiceResults childServiceResults =  uncompletedChildServices.stream()
                    .filter(ti -> !ti.getId().equals(jacsService.getId()))
                    .map(ti -> jacsServiceDataPersistence.findById(ti.getId()))
                    .collect(ChildServiceResults::new, (cr, ti) -> {
                        if (ti.hasCompletedSuccessfully()) {
                            cr.successfullyCompleted.add(ti);
                        } else if (ti.hasCompletedUnsuccessfully()) {
                            cr.unsuccessfullyCompleted.add(ti);
                        } else {
                            cr.uncompleted.add(ti);
                        }
                    }, (cr1, cr2) -> {
                        cr1.successfullyCompleted.addAll(cr2.successfullyCompleted);
                        cr1.unsuccessfullyCompleted.addAll(cr2.unsuccessfullyCompleted);
                        cr1.uncompleted.addAll(cr2.uncompleted);
                    });
            if (!childServiceResults.unsuccessfullyCompleted.isEmpty()) {
                // one of the children has not completed successfully so cancel this
                jacsService.setState(JacsServiceState.CANCELED);
                waitForChildrenToEndFuture.completeExceptionally(new ComputationException(jacsService,
                        String.format("Some child services have completed unsucessfully: %s", childServiceResults.unsuccessfullyCompleted.toString())));
                break;
            }
            uncompletedChildServices = childServiceResults.uncompleted;
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

    protected Path getWorkingDirectory(JacsService<R> jacsService) {
        String workingDir;
        if (StringUtils.isNotBlank(jacsService.getWorkspace())) {
            workingDir = jacsService.getWorkspace();
        } else if (StringUtils.isNotBlank(defaultWorkingDir)) {
            workingDir = defaultWorkingDir;
        } else {
            workingDir = System.getProperty("java.io.tmpdir");
        }
        return Paths.get(workingDir, jacsService.getName() + "_" + jacsService.getId().toString()).toAbsolutePath();
    }

}
