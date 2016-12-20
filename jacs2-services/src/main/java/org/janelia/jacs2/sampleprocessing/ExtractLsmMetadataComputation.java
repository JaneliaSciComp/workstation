package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractExternalProcessComputation;
import org.janelia.jacs2.service.impl.JacsService;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Named("lsmMetadataService")
public class ExtractLsmMetadataComputation extends AbstractExternalProcessComputation<Void> {

    private static final String PATH_VARNAME = "PATH";
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

    @Override
    public CompletionStage<JacsService<Void>> preProcessData(JacsService<Void> jacsService) {
        CompletableFuture<JacsService<Void>> preProcess = new CompletableFuture<>();
        ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArgs = getArgs(jacsService.getJacsServiceData());
        if (StringUtils.isBlank(lsmMetadataArgs.inputLSMFile)) {
            preProcess.completeExceptionally(new IllegalArgumentException("Input LSM file name must be specified"));
        } else if (StringUtils.isBlank(lsmMetadataArgs.outputLSMMetadata)) {
            preProcess.completeExceptionally(new IllegalArgumentException("Output LSM metadata name must be specified"));
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
        jacsServiceData.setServiceCmd(perlExecutable);
        ImmutableList.Builder<String> cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder
                .add(getFullExecutableName(scriptName))
                .add(StringUtils.wrapIfMissing(getInputFileName(lsmMetadataArgs), '"'))
                .add(">")
                .add(StringUtils.wrapIfMissing(getOutputFileName(lsmMetadataArgs), '"'));
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData si) {
        return ImmutableMap.of(
            PATH_VARNAME, getUpdatedEnvValue(PATH_VARNAME, perlModule),
            PERLLIB_VARNAME, getUpdatedEnvValue(PERLLIB_VARNAME, perlModule)
        );
    }

    private ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs getArgs(JacsServiceData jacsServiceData) {
        ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArgs = new ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs();
        new JCommander(lsmMetadataArgs).parse(jacsServiceData.getArgsAsArray());
        return lsmMetadataArgs;
    }

    private String getInputFileName(ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArg) {
        return new File(lsmMetadataArg.inputLSMFile).getAbsolutePath();
    }

    private String getOutputFileName(ExtractLsmMetadataServiceDescriptor.LsmMetadataArgs lsmMetadataArg) {
        return new File(lsmMetadataArg.outputLSMMetadata).getAbsolutePath();
    }
}
