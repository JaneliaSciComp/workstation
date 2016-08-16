package org.janelia.it.workstation.gui.browser.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fosterl on 8/15/2016.
 */
public class RerunSamplesAction implements NamedAction {
    private static final String TASK_LABEL = "GSPS_CompleteSamplePipeline";
    private static final int MAX_SAMPLE_RERUN_COUNT = 10;
    private List<Sample> samples;

    private static Logger logger = LoggerFactory.getLogger(RerunSamplesAction.class);

    /**
     * Returns action or null.  Action will be returned, if the selected objects contain one or more samples, the
     * user is empowered to write those samples, and there are 10 or fewer samples.
     *
     * @param selectedObjects containing 1-10 samples.
     * @return named action or null.
     */
    public static RerunSamplesAction createAction(List<DomainObject> selectedObjects) {
        RerunSamplesAction action = null;
        List<Sample> samples = new ArrayList<>();
        for (DomainObject re : selectedObjects) {
            if (re == null) {
                logger.info("Null object in selection.");
                continue;
            }
            if (re instanceof Sample) {
                Sample sample = (Sample)re;
                if (sample.getStatus() == null) {
                    logger.info("Null sample status in selection Name={}, ID={}.", sample.getName(), sample.getId());
                }
                if (!DomainConstants.VALUE_PROCESSING.equals(sample.getStatus())  &&
                    !DomainConstants.VALUE_MARKED.equals(sample.getStatus())  &&
                    ClientDomainUtils.hasWriteAccess(sample)) {
                    samples.add(sample);
                }
            }
        }
        if (samples.size() > 0  &&  samples.size() <= MAX_SAMPLE_RERUN_COUNT) {
            action = new RerunSamplesAction(samples);
        }
        return action;
    }

    /**
     * Construct with everything needed to re-run.  C'tor is private because this is intended
     * to be run only under certain criteria.
     *
     * @param samples what to re-run.
     */
    private RerunSamplesAction(List<Sample> samples) {
        this.samples = samples;
    }

    @Override
    public String getName() {
        final String samplesText = (samples.size() > 1)?samples.size()+" Samples":"Sample";
        return ("Mark "+samplesText+" for Reprocessing");
    }

    @Override
    public void doAction() {
        StringBuilder sampleText = new StringBuilder();
        if (samples.size() == 1) {
            sampleText.append("sample");
        }
        else {
            sampleText.append(samples.size());
            sampleText.append(" samples");
        }
        int result = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want the "+sampleText+" to be reprocessed?",
                "Mark for Reprocessing", JOptionPane.OK_CANCEL_OPTION);

        if (result != 0) return;

        SimpleWorker sw = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                // Force fresh pull, next attempt.
                DomainMgr.getDomainMgr().getModel().invalidate(samples);

                for (Sample sample : samples) {
                    // Wish to obtain very latest version of the sample.  Avoid letting users step on each other.
                    sample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, sample.getId());
                    if (sample.getStatus() != null  &&
                        (sample.getStatus().equals(DomainConstants.VALUE_MARKED)  ||  sample.getStatus().equals(DomainConstants.VALUE_PROCESSING))) {
                        logger.info("Bypassing sample " + sample.getName() + " because it is already marked {}.", sample.getStatus());
                        continue;
                    }

                    ActivityLogHelper.logUserAction("DomainObjectContentMenu.markForReprocessing", sample);
                    Set<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
                    taskParameters.add(new TaskParameter("reuse summary", "false", null));
                    taskParameters.add(new TaskParameter("reuse processing", "false", null));
                    taskParameters.add(new TaskParameter("reuse post", "false", null));
                    taskParameters.add(new TaskParameter("reuse alignment", "false", null));
                    Task task = new GenericTask(new HashSet<Node>(), AccessManager.getSubjectKey(), new ArrayList<Event>(),
                            taskParameters, TASK_LABEL, TASK_LABEL);
                    try {
                        task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
                        DomainMgr.getDomainMgr().getModel().updateProperty(sample, "status", DomainConstants.VALUE_MARKED);
                        ModelMgr.getModelMgr().dispatchJob(TASK_LABEL, task);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            @Override
            protected void hadSuccess() {
                logger.debug("Successfully marked samples.");
            }

            @Override
            protected void hadError(Throwable error) {
                throw new RuntimeException(error);
            }
            
        };
        sw.execute();
    }
}
