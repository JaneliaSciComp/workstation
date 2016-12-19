package org.janelia.jacs2.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ExternalProcessRunner<R> {
    CompletionStage<JacsService<R>> runCmd(String cmd, List<String> cmdArgs, Map<String, String> env, JacsService<R> serviceContext);
}
