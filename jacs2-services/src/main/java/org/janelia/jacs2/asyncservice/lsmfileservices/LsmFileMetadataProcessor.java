package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.JacsServiceDispatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;

public class LsmFileMetadataProcessor extends AbstractExeBasedServiceProcessor<File> {

    private static final String PERLLIB_VARNAME = "PERL5LIB";

    private final String perlExecutable;
    private final String perlModule;
    private final String scriptName;

    @Inject
    LsmFileMetadataProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                             ServiceComputationFactory computationFactory,
                             JacsServiceDataPersistence jacsServiceDataPersistence,
                             @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                             @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                             @Any Instance<ExternalProcessRunner> serviceRunners,
                             @PropertyValue(name = "Perl.Path") String perlExecutable,
                             @PropertyValue(name = "Sage.Perllib") String perlModule,
                             @PropertyValue(name = "LSMJSONDump.CMD") String scriptName,
                             Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.perlExecutable = perlExecutable;
        this.perlModule = perlModule;
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
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenApply(sd -> {
                    try {
                        LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args = getArgs(jacsServiceData);
                        if (StringUtils.isBlank(args.inputLSMFile)) {
                            throw new ComputationException(jacsServiceData, "Input LSM file name must be specified");
                        } else if (StringUtils.isBlank(args.outputLSMMetadata)) {
                            throw new ComputationException(jacsServiceData, "Output LSM metadata name must be specified");
                        }
                        return getOutputFile(args);
                    } catch (Exception e) {
                        logger.error("FileCopy preprocess error", e);
                        throw new ComputationException(jacsServiceData, e);
                    }
                });
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        File lsmMetadataFile = (File) preProcessingResult;
        return lsmMetadataFile.exists();
    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return (File) preProcessingResult;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        createScript(args, externalScriptCode.getCodeWriter());
        return externalScriptCode;
    }

    private void createScript(LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args, ScriptWriter scriptWriter) {
        scriptWriter
                .addWithArgs(perlExecutable)
                .addArg(getFullExecutableName(scriptName))
                .addArg(getInputFile(args).getAbsolutePath())
                .addArg(">")
                .addArg(getOutputFile(args).getAbsolutePath())
                .endArgs("");
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(
            PERLLIB_VARNAME, perlModule
        );
    }

    private LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args = new LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private File getInputFile(LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args) {
        return new File(args.inputLSMFile);
    }

    private File getOutputFile(LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args) {
        try {
            File outputFile = new File(args.outputLSMMetadata);
            Files.createDirectories(outputFile.getParentFile().toPath());
            return outputFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
