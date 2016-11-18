package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.ServiceInfo;

import java.util.List;
import java.util.Map;

public abstract class AbstractExternalProcessComputation extends AbstractServiceComputation {
    protected abstract List<String> prepareCommandLine(ServiceInfo si);
    protected abstract Map<String, String> prepareEnvironment(ServiceInfo si);
}
