package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@Named("neuronSeparatorService")
public class NeuronSeparatorComputation extends AbstractLocalProcessComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private ExecutorService serviceExecutor;

    @Override
    protected List<String> prepareCommandLine(JacsServiceData si) {
        ImmutableList.Builder cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(si.getServiceCmd());
        cmdLineBuilder.add(si.getId().toString());
        cmdLineBuilder.add(si.getName());
        if (CollectionUtils.isNotEmpty(si.getArgs())) {
            cmdLineBuilder.addAll(si.getArgs());
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData si) {
        return ImmutableMap.of();
    }

    @Override
    public CompletionStage<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        JacsServiceData childService = new JacsServiceData();
        childService.setServiceCmd("echo");
        childService.addArg("!!!!!!!!!!!!!!!!!!!!!! running as a neuron separator child service");
        childService.addArg("!!!!!!!!!!!!!!!!!!  neuron separator sub arg");
        childService.setName("sage");
        childService.setPriority(jacsServiceData.priority() + 1);

        submitChildServiceAsync(childService, jacsServiceData);
        return super.preProcessData(jacsServiceData);
    }

}
