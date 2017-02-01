package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

abstract class AbstractExternalProcessRunner implements ExternalProcessRunner {
    protected final Logger logger;

    AbstractExternalProcessRunner(Logger logger) {
        this.logger = logger;
    }

    protected String createProcessingScript(ExternalCodeBlock externalCode, String workingDirName, JacsServiceData sd) throws Exception {
        Preconditions.checkArgument(!externalCode.isEmpty());
        Preconditions.checkArgument(StringUtils.isNotBlank(workingDirName));
        Path workingDirectory = Paths.get(workingDirName);
        Files.createDirectories(workingDirectory);
        String scriptFileName = sd.getName() + "_" + sd.getId().toString() + ".sh";
        File scriptFile = new File(workingDirectory.toFile(), scriptFileName);
        ScriptWriter scriptWriter = new ScriptWriter(new BufferedWriter(new FileWriter(scriptFile)));
        try {
            scriptWriter.add(externalCode.toString());
        } finally {
            scriptWriter.close();
        }
        return scriptFile.getAbsolutePath();
    }

    protected void deleteProcessingScript(String processingScript) {
        try {
            java.nio.file.Files.deleteIfExists(new File(processingScript).toPath());
        } catch (IOException e) {
            logger.warn("Error deleting the processing script {}", processingScript, e);
        }
    }
}
