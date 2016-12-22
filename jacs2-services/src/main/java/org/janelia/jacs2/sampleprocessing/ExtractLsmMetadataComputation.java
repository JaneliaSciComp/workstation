package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("lsmMetadataService")
public class ExtractLsmMetadataComputation extends AbstractExternalProcessComputation<Void> {

    private static final String PERLLIB_VARNAME = "PERL5LIB";

    @PropertyValue(name = "Perl.Path")
    @Inject
    private String perlExecutable;
    @PropertyValue(name = "Sage.Perllib")
    @Inject
    private String perlModule;
    @PropertyValue(name = "LSMJSONDump.CMD")
    @Inject
    private String scriptName;
    @Inject
    private Logger logger;

    @Override
    public CompletionStage<JacsService<Void>> preProcessData(JacsService<Void> jacsService) {
        CompletableFuture<JacsService<Void>> preProcess = new CompletableFuture<>();
        ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArgs = getArgs(jacsService.getJacsServiceData());
        if (StringUtils.isBlank(lsmMetadataArgs.inputLSMFile)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "Input LSM file name must be specified"));
        } else if (StringUtils.isBlank(lsmMetadataArgs.outputLSMMetadata)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "Output LSM metadata name must be specified"));
        } else {
            preProcess.complete(jacsService);
        }
        return preProcess;
    }

    @Override
    public CompletionStage<JacsService<Void>> isReadyToProcess(JacsService<Void> jacsService) {
        // this service has no child services
        return CompletableFuture.completedFuture(jacsService);
    }

    @Override
    protected List<String> prepareCmdArgs(JacsServiceData jacsServiceData) {
        ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArgs = getArgs(jacsServiceData);
        File scriptFile = createScript(jacsServiceData, lsmMetadataArgs);
        jacsServiceData.setServiceCmd(scriptFile.getAbsolutePath());
        return ImmutableList.of();
    }

    private File createScript(JacsServiceData jacsServiceData, ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArgs) {
        File inputFile = getInputFile(lsmMetadataArgs);
        File outputFile = getOutputFile(lsmMetadataArgs);
        File workingDir = outputFile.getParentFile();
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
        BufferedWriter outputStream = null;
        File scriptFile = null;
        try {
            scriptFile = Files.createFile(
                    Paths.get(workingDir.getAbsolutePath(), jacsServiceData.getName() + "_" + jacsServiceData.getId() + ".sh"),
                    PosixFilePermissions.asFileAttribute(perms)).toFile();
            outputStream = new BufferedWriter(new FileWriter(scriptFile));
            outputStream.append(String.format("%s %s %s > %s\n", perlExecutable, getFullExecutableName(scriptName), inputFile.getAbsoluteFile(), outputFile.getAbsoluteFile()));
            outputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
        return scriptFile;
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData si) {
        return ImmutableMap.of(
            PERLLIB_VARNAME, perlModule
        );
    }

    private ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArgs = new ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs();
        new JCommander(lsmMetadataArgs).parse(jacsServiceData.getArgsAsArray());
        return lsmMetadataArgs;
    }

    private File getInputFile(ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArg) {
        return new File(lsmMetadataArg.inputLSMFile);
    }

    private File getOutputFile(ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArg) {
        try {
            File outputFile = new File(lsmMetadataArg.outputLSMMetadata);
            Files.createDirectories(outputFile.getParentFile().toPath());
            return outputFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void postProcessData(JacsService<Void> jacsService, Throwable exc) {
        if (exc == null) {
            try {
                Files.deleteIfExists(new File(jacsService.getJacsServiceData().getServiceCmd()).toPath());
            } catch (IOException e) {
                logger.error("Error deleting the service script {}", jacsService.getJacsServiceData().getServiceCmd(), e);
            }
        }
    }

}
