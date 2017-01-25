package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceDispatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

public class SampleSummaryProcessor extends AbstractServiceProcessor<Void> {

    private final SampleDataService sampleDataService;

    @Inject
    SampleSummaryProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                           ServiceComputationFactory computationFactory,
                           JacsServiceDataPersistence jacsServiceDataPersistence,
                           @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                           SampleDataService sampleDataService,
                           Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sampleDataService = sampleDataService;
    }

    @Override
    protected ServiceComputation<List<File>> preProcessData(JacsServiceData jacsServiceData) {
        SampleSummaryServiceDescriptor.SampleSummaryArgs args = getArgs(jacsServiceData);
        if (args.sampleId == null) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "Sample Id is required"));
        }
        JacsServiceDataBuilder sampleSummaryServiceDataBuilder = new JacsServiceDataBuilder(jacsServiceData)
                .setName("getSampleMIPsAndMovies")
                .addArg("-sampleId", args.sampleId.toString());
        if (StringUtils.isNotBlank(args.sampleObjective)) {
            sampleSummaryServiceDataBuilder.addArg("-objective", args.sampleObjective);
        }
        sampleSummaryServiceDataBuilder.addArg("-sampleDataDir", args.sampleDataDir);
        JacsServiceData sampleSummaryServiceData = sampleSummaryServiceDataBuilder.build();
        return this.submitServiceDependency(jacsServiceData, sampleSummaryServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(r -> ServiceDataUtils.stringToFileList(sampleSummaryServiceData.getStringifiedResult()));
    }

    @Override
    protected ServiceComputation<Void> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public Void getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Void result, JacsServiceData jacsServiceData) {

    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    private SampleSummaryServiceDescriptor.SampleSummaryArgs getArgs(JacsServiceData jacsServiceData) {
        SampleSummaryServiceDescriptor.SampleSummaryArgs args = new SampleSummaryServiceDescriptor.SampleSummaryArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
