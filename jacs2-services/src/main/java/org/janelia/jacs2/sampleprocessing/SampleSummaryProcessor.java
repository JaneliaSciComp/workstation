package org.janelia.jacs2.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.sample.AnatomicalArea;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.dataservice.sample.SampleDataService;
import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.janelia.jacs2.service.impl.ServiceDataUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        JacsServiceDataBuilder sampleLSMsServiceDataBuilder = new JacsServiceDataBuilder(jacsServiceData)
                .setName("sampleImageFiles")
                .addArg("-sampleId", args.sampleId.toString());
        if (StringUtils.isNotBlank(args.sampleObjective)) {
            sampleLSMsServiceDataBuilder.addArg("-objective", args.sampleObjective);
        }
        sampleLSMsServiceDataBuilder.addArg("-sampleDataDir", getWorkingDirectory(jacsServiceData).toString());
        JacsServiceData sampleLSMsServiceData = sampleLSMsServiceDataBuilder.build();
        return this.submitChildService(jacsServiceData, sampleLSMsServiceData)
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(r -> ServiceDataUtils.stringToFileList(sampleLSMsServiceData.getStringifiedResult()));
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

    private SampleSummaryServiceDescriptor.SampleSummaryArgs getArgs(JacsServiceData jacsServiceData) {
        SampleSummaryServiceDescriptor.SampleSummaryArgs args = new SampleSummaryServiceDescriptor.SampleSummaryArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}
