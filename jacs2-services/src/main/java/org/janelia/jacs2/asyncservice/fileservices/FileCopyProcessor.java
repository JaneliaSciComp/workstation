package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
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
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class FileCopyProcessor extends AbstractExeBasedServiceProcessor<File> {

    private final String libraryPath;
    private final String scriptName;

    @Inject
    FileCopyProcessor(JacsServiceEngine jacsServiceEngine,
                      ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                      @Any Instance<ExternalProcessRunner> serviceRunners,
                      @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                      @PropertyValue(name = "Convert.ScriptPath") String scriptName,
                      Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.libraryPath = libraryPath;
        this.scriptName = scriptName;
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
    protected ServiceComputation<File> preProcessData(JacsServiceData jacsServiceData) {
        try {
            FileCopyServiceDescriptor.FileCopyArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.sourceFilename)) {
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Source file name must be specified"));
            } else if (StringUtils.isBlank(args.targetFilename)) {
                return computationFactory.newFailedComputation(
                        new ComputationException(jacsServiceData, "Target file name must be specified"));
            } else {
                File targetFile = new File(args.targetFilename);
                try {
                    Files.createDirectories(targetFile.getParentFile().toPath());
                } catch (IOException e) {
                    throw new ComputationException(jacsServiceData, e);
                }
                return computationFactory.newCompletedComputation(targetFile);
            }
        } catch (Exception e) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, e));
        }
    }

    @Override
    protected ServiceComputation<File> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        File destFile = (File)preProcessingResult;
        if (destFile.exists()) {
            logger.info("Nothing to do since the destination file '{}' already exists", destFile);
            setResult(destFile, jacsServiceData);
            return computationFactory.newCompletedComputation(destFile);
        } else {
            return super.localProcessData(preProcessingResult, jacsServiceData);
        }
    }

    @Override
    protected ServiceComputation<File> postProcessData(File processingResult, JacsServiceData jacsServiceData) {
        try {
            FileCopyServiceDescriptor.FileCopyArgs args = getArgs(jacsServiceData);
            if (args.deleteSourceFile) {
                File sourceFile = new File(args.sourceFilename);
                Files.deleteIfExists(sourceFile.toPath());
            }
            return computationFactory.newCompletedComputation(processingResult);
        } catch (Exception e) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, e));
        }
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        File destFile = (File) preProcessingResult;
        return Files.exists(destFile.toPath());

    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return (File) preProcessingResult;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = getArgs(jacsServiceData);
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

    private FileCopyServiceDescriptor.FileCopyArgs getArgs(JacsServiceData jacsServiceData) {
        FileCopyServiceDescriptor.FileCopyArgs fileCopyArgs = new FileCopyServiceDescriptor.FileCopyArgs();
        new JCommander(fileCopyArgs).parse(jacsServiceData.getArgsArray());
        return fileCopyArgs;
    }

}
