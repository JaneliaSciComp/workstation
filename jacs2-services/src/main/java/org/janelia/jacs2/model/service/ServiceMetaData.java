package org.janelia.jacs2.model.service;

import java.util.List;

public class ServiceMetaData {

    private String serviceName;
    private String description;
    private List<ServiceArgDescriptor> inputArgs;
    private List<ServiceArgDescriptor> results;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ServiceArgDescriptor> getInputArgs() {
        return inputArgs;
    }

    public void setInputArgs(List<ServiceArgDescriptor> inputArgs) {
        this.inputArgs = inputArgs;
    }

    public List<ServiceArgDescriptor> getResults() {
        return results;
    }

    public void setResults(List<ServiceArgDescriptor> results) {
        this.results = results;
    }
}
