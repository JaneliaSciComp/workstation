package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.model.service.ServiceState;
import org.janelia.jacs2.persistence.ServiceInfoPersistence;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private Instance<ServiceInfoPersistence> serviceInfoPersistenceSource;

    protected ServiceInfo serviceInfo;

    @Override
    public CompletionStage<ServiceComputation> processData() {
        CompletableFuture<ServiceComputation> computationFuture;
        computationFuture = CompletableFuture.supplyAsync(() -> serviceInfo)
            .thenApplyAsync(si -> {
                ServiceState state = ServiceState.RUNNING;
                logger.info("Update state of {} to {}", si, state);
                si.setState(state);
                serviceInfoPersistenceSource.get().update(si);
                return si;
            })
            .thenApplyAsync(si -> {
                performComputation();
                return this;
            });
        return computationFuture;
    }

    protected abstract void performComputation();

    @Override
    public ServiceInfo getComputationInfo() {
        return serviceInfo;
    }

    @Override
    public void setComputationInfo(ServiceInfo si) {
        this.serviceInfo = si;
    }

}
