package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.OrderStatus;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.jacs.model.domain.orders.IntakeOrder;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.StatusTransition;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fosterl on 8/15/2016.
 */
public class RerunSamplesAction extends AbstractAction {

    private static Logger log = LoggerFactory.getLogger(RerunSamplesAction.class);
    
    private static final String TASK_LABEL = "GSPS_CompleteSamplePipeline";
    private static final int MAX_SAMPLE_RERUN_COUNT = 10;
    private List<Sample> samples;

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
                log.info("Null object in selection.");
                continue;
            }
            if (re instanceof Sample) {
                Sample sample = (Sample)re;
                if (sample.getStatus() == null) {
                    log.info("Null sample status in selection Name={}, ID={}.", sample.getName(), sample.getId());
                }
                if (!PipelineStatus.Processing.toString().equals(sample.getStatus())  &&
                    !PipelineStatus.Scheduled.toString().equals(sample.getStatus())  &&
                    ClientDomainUtils.hasWriteAccess(sample)) {
                    samples.add(sample);
                }
            }
        }
        if (samples.size() > 0  &&
            (samples.size() <= MAX_SAMPLE_RERUN_COUNT  ||  AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin))) {
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
        super(getName(samples));
        this.samples = samples;
    }

    public static final String getName(List<Sample> samples) {
        final String samplesText = (samples.size() > 1)?samples.size()+" Samples":"Sample";
        return ("Mark "+samplesText+" for Reprocessing");
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        
        StringBuilder sampleText = new StringBuilder();
        if (samples.size() == 1) {
            sampleText.append("sample");
        }
        else {
            sampleText.append(samples.size());
            sampleText.append(" samples");
        }
        int result = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), "Are you sure you want the "+sampleText+" to be reprocessed?",
                "Mark for Reprocessing", JOptionPane.OK_CANCEL_OPTION);

        if (result != 0) return;

        SimpleWorker sw = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
               
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                
                Calendar c = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("yyyyddMMhh");
                Sample sampleInfo = samples.get(0);
                String orderNo = "Workstation_" + sampleInfo.getOwnerName() + "_" +
                        sampleInfo.getDataSet() + "_" +format.format(Calendar.getInstance().getTime());

                List<Long> sampleIds = new ArrayList<>();
                for (Sample sample : samples) {
                    
                    String status = sample.getStatus();
                    if (PipelineStatus.Scheduled.toString().equals(status)  ||
                                PipelineStatus.Processing.toString().equals(status)) {
                        log.info("Bypassing sample " + sample.getName() + " because it is already marked {}.", status);
                        continue;
                    }

                    ActivityLogHelper.logUserAction("DomainObjectContentMenu.markForReprocessing", sample);
                    
                    Set<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
                    taskParameters.add(new TaskParameter("order no", orderNo, null));
                    taskParameters.add(new TaskParameter("reuse summary", "false", null));
                    taskParameters.add(new TaskParameter("reuse processing", "false", null));
                    taskParameters.add(new TaskParameter("reuse post", "false", null));
                    taskParameters.add(new TaskParameter("reuse alignment", "false", null));
                    Task task = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), 
                            new ArrayList<Event>(), taskParameters, TASK_LABEL, TASK_LABEL);
                    task = StateMgr.getStateMgr().saveOrUpdateTask(task);
                    
                    StatusTransition transition = new StatusTransition();
                    transition.setOrderNo(orderNo);
                    transition.setSource(PipelineStatus.valueOf(status));
                    transition.setProcess("Front End Processing");
                    transition.setSampleId(sample.getId());
                    transition.setTarget(PipelineStatus.Scheduled);
                    model.addPipelineStatusTransition(transition);
                    model.updateProperty(sample, "status", PipelineStatus.Scheduled.toString());
                    StateMgr.getStateMgr().dispatchJob(TASK_LABEL, task);
                    sampleIds.add(sample.getId());
                }

                // add an intake order to track all these Samples
                if (samples.size()>0) {
                    // check if there is an existing order no
                    IntakeOrder order = DomainMgr.getDomainMgr().getModel().getIntakeOrder(orderNo);
                    if (order==null) {
                        order = new IntakeOrder();
                        order.setOrderNo(orderNo);
                        order.setOwner(sampleInfo.getOwnerKey());
                        order.setStartDate(c.getTime());
                        order.setStatus(OrderStatus.Intake);
                        order.setSampleIds(sampleIds);
                    } 
                    else {
                        List<Long> currIds = order.getSampleIds();
                        currIds.addAll(sampleIds);
                        order.setSampleIds(currIds);
                    }
                    model.putOrUpdateIntakeOrder(order);
                }
            }

            @Override
            protected void hadSuccess() {
                log.debug("Successfully marked samples.");
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
            
        };
        sw.execute();
    }
}
