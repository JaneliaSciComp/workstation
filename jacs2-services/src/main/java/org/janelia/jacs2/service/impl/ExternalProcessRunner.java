package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ExternalProcessRunner {
    CompletionStage<JacsServiceData> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env, JacsServiceData serviceContext);
}
