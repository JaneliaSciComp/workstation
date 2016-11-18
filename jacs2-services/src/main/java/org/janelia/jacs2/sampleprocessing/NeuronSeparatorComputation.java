package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("neuronSeparatorService")
public class NeuronSeparatorComputation extends AbstractLocalProcessComputation {

    @Override
    protected List<String> prepareCommandLine(TaskInfo si) {
        return ImmutableList.of("echo", si.getServiceType());
    }

    @Override
    protected Map<String, String> prepareEnvironment(TaskInfo si) {
        return ImmutableMap.of();
    }
}
