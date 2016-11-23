package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.TaskInfo;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@Named("neuronSeparatorService")
public class NeuronSeparatorComputation extends AbstractLocalProcessComputation {

    @Named("SLF4J")
    @Inject
    private Logger logger;
    @Inject
    private ExecutorService taskExecutor;

    @Override
    protected List<String> prepareCommandLine(TaskInfo si) {
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
    protected Map<String, String> prepareEnvironment(TaskInfo si) {
        return ImmutableMap.of();
    }


    @Override
    public CompletionStage<TaskInfo> doWork(TaskInfo taskInfo) {
        // prepare to submit the child task
        TaskInfo subTask = new TaskInfo();
        subTask.setServiceCmd("echo");
        subTask.addArg("!!!!!!!!!!!!!!!!!!!!!! running as a neuron separator sub-task");
        subTask.addArg("!!!!!!!!!!!!!!!!!!  neuron separator sub arg");
        subTask.setName("sage");
        subTask.setPriority(taskInfo.priority() + 1);

        ServiceComputation subTaskComputation = submitSubTaskAsync(subTask);
        CompletionStage<TaskInfo> taskProcessing =
                CompletableFuture.supplyAsync(() -> {
                    logger.info("Waiting for sub-task {} of {} to finish", subTask, taskInfo);
                    subTaskComputation.getDoneChannel().take();
                    logger.info("Task {} completed, continue processing {} ", subTask, taskInfo);
                    return taskInfo;
                }, taskExecutor)
                .thenCompose(ti -> {
                    logger.info("Performing neuron separator work for {}", ti);
                    return super.doWork(ti);
                });
        return taskProcessing;
    }
}
