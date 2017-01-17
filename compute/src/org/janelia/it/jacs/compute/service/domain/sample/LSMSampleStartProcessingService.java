package org.janelia.it.jacs.compute.service.domain.sample;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Updates the status of a sample.
 */
public class LSMSampleStartProcessingService extends AbstractDomainService {
    public static final String SAMPLE_PROCESSING_JOBNAME = "GSPS_CompleteSamplePipeline";
    public static final String SAMPLE_ID_PARAM = "SAMPLE_ID"; //TODO consider moving this for common use by whole pipeline.

    public void execute() throws Exception {
        Object o = processData.getItem(SAMPLE_ID_PARAM);
        List<Long> sampleIds;
        if (o instanceof String) {
            logger.info("Sample Id input is " + o.getClass() + ", valued as " + o.toString());
            List<String> sampleIdStrings = ImmutableList.copyOf(
                    Splitter.on(',')
                            .trimResults()
                            .omitEmptyStrings()
                            .split((String) o));
            sampleIds = new ArrayList<>();
            for (String sampleIdString: sampleIdStrings) {
                sampleIds.add(Long.parseLong(sampleIdString));
            }
        }
        else {
            sampleIds = (List<Long>)processData.getItem(SAMPLE_ID_PARAM);
        }
        logger.info("Creating ProcessSample task for " + sampleIds.size() + " samples.");

        // ASSUME-FOR-NOW: owner key is to be used to find the domain objects; key is for data owner.
        DomainDAL domainDAL = DomainDAL.getInstance();
        List<Sample> samples = domainDAL.getDomainObjects(ownerKey, Sample.class.getSimpleName(), sampleIds);
        for (Sample sample: samples) {
            Task processSampleTask = createTask(sample);
            logger.info("Dispatch sample processing task " + processSampleTask.getObjectId());
            computeBean.dispatchJob(SAMPLE_PROCESSING_JOBNAME, processSampleTask.getObjectId());
        }
    }

    private Task createTask(Sample sample) throws DaoException {
        HashSet<TaskParameter> taskParameters = new HashSet<>();
        taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
        Boolean reusePipelineRuns = processData.getBoolean("REUSE_PIPELINE_RUNS");
        Boolean reuseSummary = processData.getBoolean("REUSE_SUMMARY");
        Boolean reuseProcessing = processData.getBoolean("REUSE_PROCESSING");
        Boolean reusePost = processData.getBoolean("REUSE_POST");
        Boolean reuseAlignment = processData.getBoolean("REUSE_ALIGNMENT");
        String runObjectives = processData.getString("RUN_OBJECTIVES");
        if (reusePipelineRuns) {
            taskParameters.add(new TaskParameter("reuse pipeline runs", reusePipelineRuns.toString(), null));
        }
        if (reuseSummary!=null) {
            taskParameters.add(new TaskParameter("reuse summary", reuseSummary.toString(), null));
        }
        if (reuseProcessing!=null) {
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
        }
        if (reusePost!=null) {
            taskParameters.add(new TaskParameter("reuse post", reusePost.toString(), null));
        }
        if (reuseAlignment!=null) {
            taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
        }
        if (!StringUtils.isBlank(runObjectives)) {
            taskParameters.add(new TaskParameter("run objectives", runObjectives, null));
        }
        taskParameters.add(new TaskParameter("order no", processData.getString("ORDER_NO"), null));
        GenericTask processSampleTask = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), new ArrayList<Event>(),
                taskParameters, SAMPLE_PROCESSING_JOBNAME, SAMPLE_PROCESSING_JOBNAME);
        processSampleTask.setParentTaskId(task.getObjectId());
        return computeBean.saveOrUpdateTask(processSampleTask);
    }
}
