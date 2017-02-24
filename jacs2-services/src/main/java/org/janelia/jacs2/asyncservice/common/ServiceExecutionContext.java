package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ProcessingLocation;

public class ServiceExecutionContext {
    private final JacsServiceData parentServiceData;
    private ProcessingLocation processingLocation;

    public ServiceExecutionContext(JacsServiceData parentServiceData) {
        this(parentServiceData, parentServiceData.getProcessingLocation());
    }

    public ServiceExecutionContext(JacsServiceData parentServiceData, ProcessingLocation processingLocation) {
        this.parentServiceData = parentServiceData;
        this.processingLocation = processingLocation;
    }

    public JacsServiceData getParentServiceData() {
        return parentServiceData;
    }

    public ProcessingLocation getProcessingLocation() {
        return processingLocation;
    }

    public void setProcessingLocation(ProcessingLocation processingLocation) {
        this.processingLocation = processingLocation;
    }
}
