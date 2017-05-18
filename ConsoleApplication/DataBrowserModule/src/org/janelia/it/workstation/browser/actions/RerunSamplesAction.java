package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fosterl on 8/15/2016.
 */
public class RerunSamplesAction extends AbstractAction {

    private static Logger log = LoggerFactory.getLogger(RerunSamplesAction.class);
    
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
        if (!samples.isEmpty()) {
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
        
        if (samples.size() > MAX_SAMPLE_RERUN_COUNT && !AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin)) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                    "You cannot submit more than "+MAX_SAMPLE_RERUN_COUNT+" samples for reprocessing at a time.",
                    "Too many samples selected", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
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
                for (Sample sample : samples) {
                    ActivityLogHelper.logUserAction("DomainObjectContentMenu.markForReprocessing", sample);    
                }
                DomainModel model = DomainMgr.getDomainMgr().getModel(); 
                model.dispatchSamples(DomainUtils.getReferences(samples), "User Requested Reprocessing", false);
            }

            @Override
            protected void hadSuccess() {
                log.debug("Successfully dispatched "+samples.size()+" samples.");
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
            
        };
        sw.execute();
    }
}
