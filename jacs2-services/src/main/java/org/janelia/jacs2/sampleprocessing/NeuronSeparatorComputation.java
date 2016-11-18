package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.model.service.ServiceInfo;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("neuronSeparatorService")
public class NeuronSeparatorComputation extends AbstractLocalProcessComputation {

    @Override
    protected List<String> prepareCommandLine(ServiceInfo si) {
        return ImmutableList.of("echo", si.getServiceType());
    }

    @Override
    protected Map<String, String> prepareEnvironment(ServiceInfo si) {
        return ImmutableMap.of();
    }
}
