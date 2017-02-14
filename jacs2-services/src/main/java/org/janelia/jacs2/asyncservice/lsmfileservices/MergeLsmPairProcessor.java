package org.janelia.jacs2.asyncservice.lsmfileservices;

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

public class MergeLsmPairProcessor extends AbstractExeBasedServiceProcessor<File> {

    private static final String DEFAULT_MERGE_RESULT_FILE_NAME = "merged.v3draw";

    private final String lsmMergeScript;
    private final String libraryPath;

    @Inject
    MergeLsmPairProcessor(JacsServiceEngine jacsServiceEngine,
                          ServiceComputationFactory computationFactory,
                          JacsServiceDataPersistence jacsServiceDataPersistence,
                          @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                          @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                          @Any Instance<ExternalProcessRunner> serviceRunners,
                          @PropertyValue(name = "LSMMerge.ScriptPath") String lsmMergeScript,
                          @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                          Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.lsmMergeScript = lsmMergeScript;
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
        MergeLsmPairServiceDescriptor.MergeLsmPairArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, MergeLsmPairServiceDescriptor.MergeLsmPairArgs args,
                              ScriptWriter scriptWriter) {
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
                    .addArgs(args.lsm1File, args.lsm2File)
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
        File mergedLsmResultFile = getMergedLsmResultFile(jacsServiceData);
        return mergedLsmResultFile.exists();
    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return getMergedLsmResultFile(jacsServiceData);
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

    private MergeLsmPairServiceDescriptor.MergeLsmPairArgs getArgs(JacsServiceData jacsServiceData) {
        return MergeLsmPairServiceDescriptor.MergeLsmPairArgs.parse(jacsServiceData.getArgsArray(), new MergeLsmPairServiceDescriptor.MergeLsmPairArgs());
    }

    private String getExecutable() {
        return getFullExecutableName(lsmMergeScript);
    }

    private File getMergedLsmResultFile(JacsServiceData jacsServiceData) {
        MergeLsmPairServiceDescriptor.MergeLsmPairArgs args = getArgs(jacsServiceData);
        return new File(args.resultDir, DEFAULT_MERGE_RESULT_FILE_NAME);
    }

}
