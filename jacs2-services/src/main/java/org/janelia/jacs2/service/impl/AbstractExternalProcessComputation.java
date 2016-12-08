package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractExternalProcessComputation extends AbstractServiceComputation {
    protected abstract List<String> prepareCommandLine(JacsServiceData jacsServiceData);
    protected abstract Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData);

    protected Optional<String> getEnvVar(String varName) {
        return Optional.ofNullable(System.getenv(varName));
    }

}
