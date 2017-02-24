package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;

import java.util.ArrayList;
import java.util.List;

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

        public Builder waitFor(JacsServiceData dependency) {
            serviceExecutionContext.waitFor.add(dependency);
            return this;
        }

        public ServiceExecutionContext build() {
            return serviceExecutionContext;
        }
    }

    private final JacsServiceData parentServiceData;
    private ProcessingLocation processingLocation;
    private List<JacsServiceData> waitFor = new ArrayList<>();

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
}
