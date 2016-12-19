package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.Optional;

public class JacsService<R> {

    private final JacsServiceData jacsServiceData;
    private final JacsServiceDispatcher serviceDispatcher;

    private R result;

    public JacsService(JacsServiceDispatcher serviceDispatcher, JacsServiceData jacsServiceData) {
        this.serviceDispatcher = serviceDispatcher;
        this.jacsServiceData = jacsServiceData;
    }

    public ServiceComputation<?> submitChildServiceAsync(JacsServiceData childServiceData) {
        if (childServiceData.priority() <= jacsServiceData.priority()) {
            // child services must have a higher priority, otherwise we may run into deadlocks
            childServiceData.setPriority(jacsServiceData.priority() + 1);
        }
        JacsServiceData serviceData = serviceDispatcher.submitServiceAsync(childServiceData, Optional.of(jacsServiceData));
        return serviceDispatcher.getServiceComputation(serviceData);
    }

    public JacsServiceData getJacsServiceData() {
        return jacsServiceData;
    }

    public R getResult() {
        return result;
    }

    public void setResult(R result) {
        this.result = result;
    }

    public String getOwner() {
        return jacsServiceData.getOwner();
    }
}
