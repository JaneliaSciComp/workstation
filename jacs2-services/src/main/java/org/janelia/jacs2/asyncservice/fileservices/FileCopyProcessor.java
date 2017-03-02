package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Named("fileCopy")
public class FileCopyProcessor extends AbstractExeBasedServiceProcessor<File> {

    public static class FileCopyArgs extends ServiceArgs {
        @Parameter(names = "-src", description = "Source file name", required = true)
        String sourceFilename;
        @Parameter(names = "-dst", description = "Destination file name or location", required = true)
        String targetFilename;
        @Parameter(names = "-mv", arity = 0, description = "If used the file will be moved to the target", required = false)
        boolean deleteSourceFile = false;
        @Parameter(names = "-convert8", arity = 0, description = "If set it converts the image to 8bit", required = false)
        boolean convertTo8Bits = false;
    }

    private final String libraryPath;
    private final String scriptName;

    @Inject
    FileCopyProcessor(JacsServiceEngine jacsServiceEngine,
                      ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                      @Any Instance<ExternalProcessRunner> serviceRunners,
                      @PropertyValue(name = "VAA3D.Library.Path") String libraryPath,
                      @PropertyValue(name = "Convert.ScriptPath") String scriptName,
                      Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.libraryPath = libraryPath;
        this.scriptName = scriptName;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FileCopyArgs());
    }

    @Override
    public File getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFile(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(File result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileToString(result));
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        try {
            FileCopyArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.sourceFilename)) {
                return createFailure(jacsServiceData, new ComputationException(jacsServiceData, "Source file name must be specified"));
            } else if (StringUtils.isBlank(args.targetFilename)) {
                return createFailure(jacsServiceData, new ComputationException(jacsServiceData, "Target file name must be specified"));
            } else {
                File targetFile = getTargetFile(args);
                try {
                    Files.createDirectories(targetFile.getParentFile().toPath());
                } catch (IOException e) {
                    return createFailure(jacsServiceData, e);
                }
                return createComputation(jacsServiceData);
            }
        } catch (Exception e) {
            return createFailure(jacsServiceData, e);
        }
    }

    @Override
    protected ServiceComputation<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        FileCopyArgs args = getArgs(jacsServiceData);
        File destFile = getTargetFile(args);
        if (destFile.exists()) {
            logger.info("Nothing to do since the destination file '{}' already exists", destFile);
            return createComputation(jacsServiceData);
        } else {
            return super.processData(jacsServiceData);
        }
    }

    @Override
    protected ServiceComputation<JacsServiceData> postProcessData(JacsServiceData jacsServiceData) {
        try {
            FileCopyArgs args = getArgs(jacsServiceData);
            if (args.deleteSourceFile) {
                File sourceFile = getSourceFile(args);
                Files.deleteIfExists(sourceFile.toPath());
            }
            return createComputation(jacsServiceData);
        } catch (Exception e) {
            return createFailure(jacsServiceData, e);
        }
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        FileCopyArgs args = getArgs(jacsServiceData);
        File destFile = getTargetFile(args);
        return Files.exists(destFile.toPath());
    }

    @Override
    protected File retrieveResult(JacsServiceData jacsServiceData) {
        FileCopyArgs args = getArgs(jacsServiceData);
        return getTargetFile(args);
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        FileCopyArgs fileCopyArgs = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter codeWriter = externalScriptCode.getCodeWriter();
        codeWriter
                .addWithArgs(getFullExecutableName(scriptName))
                .addArg(fileCopyArgs.sourceFilename)
                .addArg(fileCopyArgs.targetFilename);
        if (fileCopyArgs.convertTo8Bits) {
            codeWriter.addArg("8");
        }
        codeWriter.endArgs("");
        return externalScriptCode;
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private FileCopyArgs getArgs(JacsServiceData jacsServiceData) {
        FileCopyArgs fileCopyArgs = new FileCopyArgs();
        new JCommander(fileCopyArgs).parse(jacsServiceData.getArgsArray());
        return fileCopyArgs;
    }

    private File getSourceFile(FileCopyArgs args) {
        return new File(args.sourceFilename);
    }

    private File getTargetFile(FileCopyArgs args) {
        return new File(args.targetFilename);
    }

}
