package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.util.List;
import java.util.Map;

public interface ExternalProcessRunner {
    void runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                String workingDirName,
                ExternalProcessOutputHandler outStreamHandler,
                ExternalProcessOutputHandler errStreamHandler,
                JacsServiceData serviceContext);
}
