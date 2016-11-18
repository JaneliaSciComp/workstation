package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.ServiceComputation;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@Named("neuronSeparatorService")
public class NeuronSeparatorComputation extends AbstractLocalProcessComputation {

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


    @Override
    public TaskInfo doWork(TaskInfo taskInfo) {
        // prepare to submit the child task
        TaskInfo subTask = new TaskInfo();
        subTask.setServiceCmd("echo");
        subTask.addArg("!!!!!!!!!!!!!!!!!!!!!! running as a neuron separator sub-task");
        subTask.addArg("!!!!!!!!!!!!!!!!!!  neuron separator sub arg");
        subTask.setName("sage");

        ServiceComputation subTaskComputation = submitSubTaskAsync(subTask);
        CompletableFuture<TaskInfo> taskProcessing = subTaskComputation.processData()
                .thenApply(ti -> {
                    return super.doWork(taskInfo);
                })
                .toCompletableFuture();
        try {
            return taskProcessing.get();
        } catch (Exception e) {
            throw new ComputationException(e);
        }
    }
}
