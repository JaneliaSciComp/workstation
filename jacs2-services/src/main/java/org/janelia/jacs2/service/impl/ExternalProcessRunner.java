package org.janelia.jacs2.service.impl;

import org.janelia.jacs2.model.service.JacsServiceData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ExternalProcessRunner {
    void runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                String workingDirName,
                ExternalProcessOutputHandler outStreamHandler,
                ExternalProcessOutputHandler errStreamHandler,
                JacsServiceData serviceContext);
}
