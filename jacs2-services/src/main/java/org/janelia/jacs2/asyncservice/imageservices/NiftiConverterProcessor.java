package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceCommand;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Named("niftiConverter")
public class NiftiConverterProcessor extends AbstractBasicLifeCycleServiceProcessor<List<File>> implements ServiceCommand {

    static class Vaa3dNiftiConverterArgs extends ServiceArgs {
        @Parameter(names = "-input", description = "Input file", required = true)
        List<String> inputFileNames = new ArrayList<>();
        @Parameter(names = "-output", description = "Output file", required = true)
        List<String> outputFileNames;
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    NiftiConverterProcessor(JacsServiceEngine jacsServiceEngine,
                            ServiceComputationFactory computationFactory,
                            JacsServiceDataPersistence jacsServiceDataPersistence,
                            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                            Logger logger,
                            Vaa3dPluginProcessor vaa3dPluginProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dNiftiConverterArgs());
    }

    @Override
    public List<File> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFileList(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(List<File> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileListToString(result));
    }

    @Override
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
            if (CollectionUtils.isEmpty(args.inputFileNames)) {
                return createFailure(new ComputationException(jacsServiceData, "An input file name must be specified"));
            }
            return createComputation(jacsServiceData);
        } catch (Exception e) {
            return createFailure(e);
        }
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
        return ImmutableList.of(submitVaa3dPluginService(args, jacsServiceData, JacsServiceState.QUEUED));
    }

    @Override
    protected ServiceComputation<List<File>> processing(JacsServiceData jacsServiceData) {
        return createComputation(this.waitForResult(jacsServiceData));
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return checkForDependenciesCompletion(jacsServiceData);
    }

    @Override
    protected List<File> retrieveResult(JacsServiceData jacsServiceData) {
        Vaa3dNiftiConverterArgs args = getArgs(jacsServiceData);
        List<File> results = new ArrayList<>();
        args.outputFileNames.forEach(o -> {
            File oFile = new File(o);
            if (oFile.exists()) {
                results.add(oFile);
            }
        });
        return results;
    }

    private JacsServiceData submitVaa3dPluginService(Vaa3dNiftiConverterArgs args, JacsServiceData jacsServiceData, JacsServiceState vaa3dPluginServiceState) {
        return submit(vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData).state(vaa3dPluginServiceState).build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "NiftiImageConverter"),
                new ServiceArg("-input", String.join(",", args.inputFileNames)),
                new ServiceArg("-output", String.join(",", args.outputFileNames))
        ));
    }

    @Override
    public void execute(JacsServiceData jacsServiceData) {
        execute(sd -> {
            Vaa3dNiftiConverterArgs args = getArgs(sd);
            vaa3dPluginProcessor.execute(submitVaa3dPluginService(args, sd, JacsServiceState.RUNNING));
        }, jacsServiceData);
    }

    private Vaa3dNiftiConverterArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dNiftiConverterArgs args = new Vaa3dNiftiConverterArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
