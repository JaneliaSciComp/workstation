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
        JacsServiceData serviceData = serviceDispatcher.submitServiceAsync(childServiceData);
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

    public Number getId() {
        return jacsServiceData.getId();
    }

    public String getName() {
        return jacsServiceData.getName();
    }

    public String[] getArgsArray() {
        return jacsServiceData.getArgsArray();
    }

    public String getOwner() {
        return jacsServiceData.getOwner();
    }

    public String getServiceCmd() {
        return jacsServiceData.getServiceCmd();
    }

    public void setServiceCmd(String serviceCmd) {
        jacsServiceData.setServiceCmd(serviceCmd);
    }

    public String getServiceType() {
        return jacsServiceData.getServiceType();
    }

}
