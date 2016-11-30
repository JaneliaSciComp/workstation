package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("flylightSamplePipelineService")
public class FlylightSamplePipelineComputation extends AbstractLocalProcessComputation {

    @Override
    protected List<String> prepareCommandLine(TaskInfo taskInfo) {
        ImmutableList.Builder cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(taskInfo.getServiceCmd());
        cmdLineBuilder.add(taskInfo.getId().toString());
        cmdLineBuilder.add(taskInfo.getName());
        if (CollectionUtils.isNotEmpty(taskInfo.getArgs())) {
            cmdLineBuilder.addAll(taskInfo.getArgs());
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(TaskInfo si) {
        return ImmutableMap.of();
    }
}
