package org.janelia.jacs2.lsmfileservices;

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

@Named("lsmFileMetadataService")
public class LsmFileMetadataComputation extends AbstractExternalProcessComputation<File> {

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
    public CompletionStage<JacsService<File>> preProcessData(JacsService<File> jacsService) {
        CompletableFuture<JacsService<File>> preProcess = new CompletableFuture<>();
        LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args = getArgs(jacsService);
        if (StringUtils.isBlank(args.inputLSMFile)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "Input LSM file name must be specified"));
        } else if (StringUtils.isBlank(args.outputLSMMetadata)) {
            preProcess.completeExceptionally(new ComputationException(jacsService, "Output LSM metadata name must be specified"));
        } else {
            preProcess.complete(jacsService);
        }
        return preProcess;
    }

    @Override
    protected List<String> prepareCmdArgs(JacsService<File> jacsService) {
        LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args = getArgs(jacsService);
        File scriptFile = createScript(jacsService, args);
        jacsService.setServiceCmd(scriptFile.getAbsolutePath());
        return ImmutableList.of();
    }

    private File createScript(JacsService<File> jacsService, LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args) {
        File inputFile = getInputFile(args);
        File outputFile = getOutputFile(args);
        File workingDir = outputFile.getParentFile();
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
        BufferedWriter outputStream = null;
        File scriptFile = null;
        try {
            scriptFile = Files.createFile(
                    Paths.get(workingDir.getAbsolutePath(), jacsService.getName() + "_" + jacsService.getId() + ".sh"),
                    PosixFilePermissions.asFileAttribute(perms)).toFile();
            outputStream = new BufferedWriter(new FileWriter(scriptFile));
            outputStream.append(String.format("%s %s %s > %s\n", perlExecutable, getFullExecutableName(scriptName), inputFile.getAbsoluteFile(), outputFile.getAbsoluteFile()));
            outputStream.flush();
            jacsService.setResult(outputFile);
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
    protected Map<String, String> prepareEnvironment(JacsService<File> jacsService) {
        return ImmutableMap.of(
            PERLLIB_VARNAME, perlModule
        );
    }

    private LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs getArgs(JacsService<File> jacsService) {
        LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs args = new LsmFileMetadataServiceDescriptor.LsmFileMetadataArgs();
        new JCommander(args).parse(jacsService.getArgsArray());
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

    @Override
    public void postProcessData(JacsService<File> jacsService, Throwable exc) {
        if (exc == null) {
            try {
                Files.deleteIfExists(new File(jacsService.getJacsServiceData().getServiceCmd()).toPath());
            } catch (IOException e) {
                logger.error("Error deleting the service script {}", jacsService.getJacsServiceData().getServiceCmd(), e);
            }
        }
    }

}
