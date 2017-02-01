package org.janelia.jacs2.asyncservice.common;

import org.janelia.jacs2.model.jacsservice.JacsServiceData;

import java.util.List;
import java.util.Map;

public interface ExternalProcessRunner {
    /**
     *  run a list of commands in the same processing context (machine + environment)
     * @param externalCode
     * @param env
     * @param workingDirName
     * @param outStreamHandler
     * @param errStreamHandler
     * @param serviceContext
     */
    void runCmds(ExternalCodeBlock externalCode,
                 Map<String, String> env,
                 String workingDirName,
                 ExternalProcessOutputHandler outStreamHandler,
                 ExternalProcessOutputHandler errStreamHandler,
                 JacsServiceData serviceContext);
}
