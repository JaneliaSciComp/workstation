package org.janelia.jacs2.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ExternalProcessRunner {
    <R> CompletionStage<JacsService<R>> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env,
                                               String workingDirName,
                                               ExternalProcessOutputHandler outStreamHandler,
                                               ExternalProcessOutputHandler errStreamHandler,
                                               JacsService<R> serviceContext);
}
