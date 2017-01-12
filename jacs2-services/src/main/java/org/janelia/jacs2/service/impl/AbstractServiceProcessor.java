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
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractServiceProcessor<R> implements ServiceProcessor<R> {

    private static class ChildServiceResults {
        private List<JacsServiceData> successfullyCompleted = new ArrayList<>();
        private List<JacsServiceData> unsuccessfullyCompleted = new ArrayList<>();
        private List<JacsServiceData> uncompleted = new ArrayList<>();
    }

    private final ServiceComputationFactory computationFactory;
    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final String defaultWorkingDir;
    private final Logger logger;

    @Inject
    public AbstractServiceProcessor(ServiceComputationFactory computationFactory, JacsServiceDataPersistence jacsServiceDataPersistence, @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir, Logger logger) {
        this.computationFactory= computationFactory;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.defaultWorkingDir = defaultWorkingDir;
        this.logger = logger;
    }

    @Override
    public ServiceComputation<ServiceProcessor<R>, ? extends R> process(JacsService<R> jacsService) {
        return preProcessData(jacsService)
                .thenCompose(this, new BiFunction<ServiceProcessor<R>, Object, ServiceComputation<ServiceProcessor<R>, Object>>() {
                    @Override
                    public ServiceComputation<ServiceProcessor<R>, Object> apply(ServiceProcessor<R> rAbstractServiceProcessor, Object o) {
                        return (ServiceComputation<ServiceProcessor<R>, Object>) ((AbstractServiceProcessor<R>)rAbstractServiceProcessor).localProcessData(jacsService);
                    }
                });
    }

    protected <R1> ServiceComputation<ServiceProcessor<R>, R1> preProcessData(JacsService<R> jacsService) {
        ServiceComputation<ServiceProcessor<R>, R1> preProcessing = computationFactory.newComputation(this);
        // the default operation is a noop
        preProcessing.applyFunction(s -> null);
        return preProcessing;
    }

    protected abstract ServiceComputation<ServiceProcessor<R>, R> localProcessData(JacsService<R> jacsService);



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
