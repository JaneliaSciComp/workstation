package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("sageService")
public class SageComputation extends AbstractLocalProcessComputation {

    @Override
    protected List<String> prepareCommandLine() {
        ImmutableList.Builder cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(getComputationInfo().getServiceCmd());
        if (CollectionUtils.isNotEmpty(getComputationInfo().getArgs())) {
            cmdLineBuilder.addAll(getComputationInfo().getArgs());
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment() {
        return ImmutableMap.of();
    }
}
