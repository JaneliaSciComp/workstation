package org.janelia.jacs2.asyncservice.imageservices;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.asyncservice.utils.X11Utils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class StitchGroupingProcessor extends AbstractExeBasedServiceProcessor<File> {

    private static final String DEFAULT_GROUP_RESULT_FILE_NAME = "igroups.txt";

    private final String vaa3dExecutable;
    private final String libraryPath;

    @Inject
    StitchGroupingProcessor(JacsServiceEngine jacsServiceEngine,
                            ServiceComputationFactory computationFactory,
                            JacsServiceDataPersistence jacsServiceDataPersistence,
                            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                            @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                            @Any Instance<ExternalProcessRunner> serviceRunners,
                            @PropertyValue(name = "VAA3D.Bin.Path") String vaa3dExecutable,
                            @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                            Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.vaa3dExecutable = vaa3dExecutable;
        this.libraryPath = libraryPath;
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
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        StitchGroupingServiceDescriptor.StitchGroupingArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, StitchGroupingServiceDescriptor.StitchGroupingArgs args,
                              ScriptWriter scriptWriter) {
        try {
            Path workingDir = getWorkingDirectory(jacsServiceData);
            X11Utils.setDisplayPort(workingDir.toString(), scriptWriter);
            File resultDir = new File(args.resultDir);
            Files.createDirectories(resultDir.toPath());
            scriptWriter.addWithArgs(getExecutable())
                    .addArgs("-x", "imageStitch.so")
                    .addArgs("-f", "istitch-grouping")
                    .addArgs("-p", "#c", String.valueOf(args.referenceChannelIndex))
                    .addArgs("-i", args.inputDir)
                    .addArgs("-o", args.resultDir)
                    .endArgs("");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        File mergedLsmResultFile = getGroupResultFile(jacsServiceData);
        return mergedLsmResultFile.exists();
    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return getGroupResultFile(jacsServiceData);
    }

    @Override
    protected boolean hasErrors(String l) {
        boolean result = super.hasErrors(l);
        if (result) {
            return true;
        }
        if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(fail to call the plugin).*)")) {
            logger.error(l);
            return true;
        } else {
            return false;
        }
    }

    private StitchGroupingServiceDescriptor.StitchGroupingArgs getArgs(JacsServiceData jacsServiceData) {
        return StitchGroupingServiceDescriptor.StitchGroupingArgs.parse(jacsServiceData.getArgsArray(), new StitchGroupingServiceDescriptor.StitchGroupingArgs());
    }

    private String getExecutable() {
        return getFullExecutableName(vaa3dExecutable);
    }

    private File getGroupResultFile(JacsServiceData jacsServiceData) {
        StitchGroupingServiceDescriptor.StitchGroupingArgs args = getArgs(jacsServiceData);
        return new File(args.resultDir, DEFAULT_GROUP_RESULT_FILE_NAME);
    }

}
