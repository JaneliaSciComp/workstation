package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("sageService")
public class SageComputation extends AbstractLocalProcessComputation {

    @Override
    protected List<String> prepareCommandLine(TaskInfo si) {
        ImmutableList.Builder cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(si.getServiceCmd());
        if (CollectionUtils.isNotEmpty(si.getArgs())) {
            cmdLineBuilder.addAll(si.getArgs());
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(TaskInfo si) {
        return ImmutableMap.of();
    }
}
