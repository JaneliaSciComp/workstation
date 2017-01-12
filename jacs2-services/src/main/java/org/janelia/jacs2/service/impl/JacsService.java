package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceState;
import org.janelia.jacs2.model.service.ProcessingLocation;

public class JacsService<R> {

    private final JacsServiceData jacsServiceData;
    private final JacsServiceDispatcher serviceDispatcher;
    private R result;
    private Throwable exception;

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
        if (result != null) {
            jacsServiceData.setStringifiedResult(result.toString());
        } else {
            jacsServiceData.setStringifiedResult(null);
        }
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
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

    public void setState(JacsServiceState state) {
        jacsServiceData.setState(state);
    }

    public ProcessingLocation getProcessingLocation() {
        return jacsServiceData.getProcessingLocation();
    }

    public String getWorkspace() {
        return jacsServiceData.getWorkspace();
    }

    @Override
    public String toString() {
        return jacsServiceData.toString();
    }
}
