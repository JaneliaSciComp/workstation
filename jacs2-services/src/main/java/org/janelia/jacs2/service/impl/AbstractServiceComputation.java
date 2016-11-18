package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.persistence.ServiceInfoPersistence;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractServiceComputation implements ServiceComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private JacsServiceDispatcher serviceDispatcher;
    @Inject
    private Instance<ServiceInfoPersistence> serviceInfoPersistenceSource;
    private final ServiceSupplier<ServiceInfo> serviceSupplier = new BlockingQueueServiceSupplier<>();
    private ServiceInfo serviceInfo;

    @Override
    public CompletionStage<ServiceInfo> processData() {
        return waitForData()
                .thenApply(this::doWork);
    }

    private CompletionStage<ServiceInfo> waitForData() {
        return CompletableFuture.supplyAsync(() -> {
            // bring the service info into the computation by getting it from the supplier channel;
            // the channel will receive the data when the corresponding job is ready to be started
            // from the dispatcher
            if (this.serviceInfo == null) {
                this.serviceInfo = serviceSupplier.take();
            }
            return this.serviceInfo;
        });
    }

    protected abstract ServiceInfo doWork(ServiceInfo si);

    @Override
    public ServiceComputation submitChildProcess(ServiceInfo si) {
        Preconditions.checkState(serviceInfo != null, "Children services can only be created once it's running");
        return serviceDispatcher.submitService(si, Optional.of(serviceInfo));
    }

    @Override
    public ServiceSupplier<ServiceInfo> getServiceSupplier() {
        return serviceSupplier;
    }

}
