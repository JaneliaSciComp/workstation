package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

abstract class AbstractExternalProcessRunner implements ExternalProcessRunner {
    private static final int MAX_SUBSCRIPT_INDEX = 100;

    protected final JacsServiceDataPersistence jacsServiceDataPersistence;
    protected final Logger logger;

    AbstractExternalProcessRunner(JacsServiceDataPersistence jacsServiceDataPersistence, Logger logger) {
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.logger = logger;
    }

    protected String createProcessingScript(ExternalCodeBlock externalCode, String workingDirName, JacsServiceData sd) {
        ScriptWriter scriptWriter = null;
        try {
            Preconditions.checkArgument(!externalCode.isEmpty());
            Preconditions.checkArgument(StringUtils.isNotBlank(workingDirName));
            Path workingDirectory = Paths.get(workingDirName);
            Files.createDirectories(workingDirectory);
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
            Path scriptFilePath = createScriptFileName(sd, workingDirectory);
            File scriptFile = Files.createFile(scriptFilePath, PosixFilePermissions.asFileAttribute(perms)).toFile();
            scriptWriter = new ScriptWriter(new BufferedWriter(new FileWriter(scriptFile)));
            scriptWriter.add(externalCode.toString());
            sd.addEvent(JacsServiceEventTypes.CREATED_RUNNING_SCRIPT, String.format("Created the running script for %s: %s", sd.getName(), sd.getArgs()));
            return scriptFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("Error creating the processing script with {} for {}", externalCode, sd, e);
            sd.addEvent(JacsServiceEventTypes.SCRIPT_CREATION_ERROR, String.format("Error creating the running script for %s: %s", sd.getName(), sd.getArgs()));
            throw new ComputationException(sd, e);
        } finally {
            if (scriptWriter != null) scriptWriter.close();
        }
    }

    private Path createScriptFileName(JacsServiceData sd, Path dir) {
        String nameSuffix = "";
        if (sd.hasId()) {
            nameSuffix = sd.getId().toString();
            Path scriptPath = checkScriptFile(dir, sd.getName(), nameSuffix);
            if (scriptPath != null) return scriptPath;
        } else if (sd.hasParentServiceId()) {
            nameSuffix = sd.getParentServiceId().toString();
        } else {
            nameSuffix = String.valueOf(System.currentTimeMillis());
        }
        for (int i = 1; i <= MAX_SUBSCRIPT_INDEX; i++) {
            Path scriptFilePath = checkScriptFile(dir, sd.getName(), nameSuffix + "_" + i);
            if (scriptFilePath != null) return scriptFilePath;
        }
        throw new ComputationException(sd, "Could not create unique script name for " + sd.getName());
    }

    private Path checkScriptFile(Path dir, String name, String suffix) {
        String nameCandidate = name + "_" + suffix + ".sh";
        Path scriptFilePath = dir.resolve(nameCandidate);
        if (!Files.exists(scriptFilePath)) {
            return scriptFilePath;
        } else {
            return null;
        }
    }
}
