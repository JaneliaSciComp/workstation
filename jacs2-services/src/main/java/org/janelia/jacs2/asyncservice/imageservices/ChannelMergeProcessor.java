package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.asyncservice.utils.X11Utils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Named("mergeChannels")
public class ChannelMergeProcessor extends AbstractExeBasedServiceProcessor<File> {

    static class ChannelMergeArgs extends ServiceArgs {
        @Parameter(names = "-chInput1", description = "File containing the first set of channels", required = true)
        String chInput1;
        @Parameter(names = "-chInput2", description = "File containing the second set of channels", required = true)
        String chInput2;
        @Parameter(names = "-multiscanVersion", description = "Multiscan blend version", required = false)
        String multiscanBlendVersion;
        @Parameter(names = "-resultDir", description = "Result directory", required = true)
        String resultDir;
    }

    private static final String DEFAULT_MERGE_RESULT_FILE_NAME = "merged.v3draw";

    private final String lsmMergeScript;
    private final String libraryPath;

    @Inject
    ChannelMergeProcessor(JacsServiceEngine jacsServiceEngine,
                          ServiceComputationFactory computationFactory,
                          JacsServiceDataPersistence jacsServiceDataPersistence,
                          @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                          @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                          @Any Instance<ExternalProcessRunner> serviceRunners,
                          @PropertyValue(name = "LSMMerge.ScriptPath") String lsmMergeScript,
                          @PropertyValue(name = "VAA3D.Library.Path") String libraryPath,
                          Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.lsmMergeScript = lsmMergeScript;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new ChannelMergeArgs());
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
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        File mergedLsmResultFile = getMergedLsmResultFile(jacsServiceData);
        return mergedLsmResultFile.exists();
    }

    @Override
    protected File retrieveResult(JacsServiceData jacsServiceData) {
        return getMergedLsmResultFile(jacsServiceData);
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        ChannelMergeArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, ChannelMergeArgs args, ScriptWriter scriptWriter) {
        try {
            Path workingDir = getWorkingDirectory(jacsServiceData);
            X11Utils.setDisplayPort(workingDir.toString(), scriptWriter);
            File resultDir = new File(args.resultDir);
            Files.createDirectories(resultDir.toPath());
            scriptWriter.addWithArgs(getExecutable())
                    .addArgs("-o", resultDir.getAbsolutePath());
            if (StringUtils.isNotBlank(args.multiscanBlendVersion)) {
                scriptWriter.addArgs("-m", args.multiscanBlendVersion);
            }
            scriptWriter
                    .addArgs(args.chInput1, args.chInput2)
                    .endArgs("");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private ChannelMergeArgs getArgs(JacsServiceData jacsServiceData) {
        return ChannelMergeArgs.parse(jacsServiceData.getArgsArray(), new ChannelMergeArgs());
    }

    private String getExecutable() {
        return getFullExecutableName(lsmMergeScript);
    }

    private File getMergedLsmResultFile(JacsServiceData jacsServiceData) {
        ChannelMergeArgs args = getArgs(jacsServiceData);
        return new File(args.resultDir, DEFAULT_MERGE_RESULT_FILE_NAME);
    }

}
