package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JacsServiceDispatcher {

    private static final int DISPATCH_BATCH_SIZE = 20;

    private final ServiceComputationFactory serviceComputationFactory;
    private final JacsServiceQueue jacsServiceQueue;
    private final JacsServiceDataPersistence jacsServiceDataPersistence;
    private final JacsServiceEngine jacsServiceEngine;
    private final Logger logger;

    @Inject
    public JacsServiceDispatcher(ServiceComputationFactory serviceComputationFactory,
                                 JacsServiceQueue jacsServiceQueue,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 JacsServiceEngine jacsServiceEngine,
                                 Logger logger) {
        this.serviceComputationFactory = serviceComputationFactory;
        this.jacsServiceQueue = jacsServiceQueue;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.jacsServiceEngine = jacsServiceEngine;
        this.logger = logger;
    }

    void dispatchServices() {
        logger.debug("Dispatch services");
        for (int i = 0; i < DISPATCH_BATCH_SIZE; i++) {
            if (!jacsServiceEngine.acquireSlot()) {
                logger.info("No available processing slots");
                return; // no slot available
            }
            JacsServiceData queuedService = jacsServiceQueue.dequeService();
            logger.debug("Dequeued service {}", queuedService);
            if (queuedService == null) {
                // nothing to do
                jacsServiceEngine.releaseSlot();
                return;
            }
            logger.info("Dispatch service {}", queuedService);
            ServiceProcessor<?> serviceProcessor = jacsServiceEngine.getServiceProcessor(queuedService);
            serviceComputationFactory.<JacsServiceData>newComputation()
                    .supply(() -> {
                        JacsServiceData service = queuedService;
                        logger.debug("Submit {}", service);
                        service.setState(JacsServiceState.SUBMITTED);
                        updateServiceInfo(service);
                        jacsServiceEngine.releaseSlot();
                        return service;
                    })
                    .thenCompose(sd -> serviceProcessor.process(sd))
                    .whenComplete((r, exc) -> jacsServiceQueue.completeService(queuedService));
        }
    }

    private void updateServiceInfo(JacsServiceData jacsServiceData) {
        jacsServiceDataPersistence.update(jacsServiceData);
    }

}
