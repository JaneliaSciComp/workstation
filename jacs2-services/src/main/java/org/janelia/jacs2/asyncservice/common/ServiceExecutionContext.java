package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceExecutionContext {

    public static class Builder {
        private final ServiceExecutionContext serviceExecutionContext;

        public Builder(JacsServiceData parentServiceData) {
            serviceExecutionContext = new ServiceExecutionContext(parentServiceData);
        }

        public Builder processingLocation(ProcessingLocation processingLocation) {
            serviceExecutionContext.processingLocation = processingLocation;
            return this;
        }

        public Builder waitFor(JacsServiceData... dependencies) {
            for (JacsServiceData dependency : dependencies) {
                if (dependency != null) serviceExecutionContext.waitFor.add(dependency);
            }
            return this;
        }

        public Builder state(JacsServiceState state) {
            serviceExecutionContext.serviceState = state;
            return this;
        }

        public Builder setServiceName(String serviceName) {
            serviceExecutionContext.serviceName = serviceName;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            serviceExecutionContext.outputPath = outputPath;
            return this;
        }

        public Builder setErrorPath(String errorPath) {
            serviceExecutionContext.errorPath = errorPath;
            return this;
        }

        public Builder description(String description) {
            serviceExecutionContext.description = description;
            return this;
        }

        public Builder addResource(String name, String value) {
            serviceExecutionContext.resources.put(name, value);
            return this;
        }

        public ServiceExecutionContext build() {
            return serviceExecutionContext;
        }
    }

    private final JacsServiceData parentServiceData;
    private ProcessingLocation processingLocation;
    private String serviceName;
    private String outputPath;
    private String errorPath;
    private JacsServiceState serviceState;
    private String description;
    private List<JacsServiceData> waitFor = new ArrayList<>();
    private Map<String, String> resources = new LinkedHashMap<>();

    public ServiceExecutionContext(JacsServiceData parentServiceData) {
        this.parentServiceData = parentServiceData;
    }

    public JacsServiceData getParentServiceData() {
        return parentServiceData;
    }

    public ProcessingLocation getProcessingLocation() {
        return processingLocation;
    }

    public List<JacsServiceData> getWaitFor() {
        return waitFor;
    }

    public JacsServiceState getServiceState() {
        return serviceState;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getResources() {
        return resources;
    }
}
