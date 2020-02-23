package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "PurgeAndBlockAction"
)
@ActionRegistration(
        displayName = "#CTL_PurgeAndBlockAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/Sample", position = 530)
})
@NbBundle.Messages("CTL_PurgeAndBlockAction=Purge And Block Sample")
public class PurgeAndBlockAction extends BaseContextualNodeAction {

    private Collection<Sample> samples = new ArrayList<>();

    @Override
    protected void processContext() {
        samples.clear();
        setVisible(false);
        if (getNodeContext().isOnlyObjectsOfType(Sample.class)) {
            for (Sample sample : getNodeContext().getOnlyObjectsOfType(Sample.class)) {
                setVisible(true);
                if (ClientDomainUtils.hasWriteAccess(sample)) {
                    samples.add(sample);
                }
            }
        }
        setEnabled(!samples.isEmpty());
    }

    @Override
    public String getName() {
        if (getViewerContext()!=null) {
            String samplesText = getViewerContext().isMultiple()?samples.size()+" Samples":"Sample";
            return "Purge And Block "+samplesText;
        }
        return "Purge And Block Sample";
    }

    @Override
    public void performAction() {

        Collection<Sample> samples = new ArrayList<>(this.samples);

        ActivityLogHelper.logUserAction("ProcessingBlockAction.actionPerformed");

        int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                "Are you sure you want to purge " + samples.size() + " sample(s) " +
                        "by deleting all large files associated with them, and block all future processing?",
                "Purge And Block Processing", JOptionPane.OK_CANCEL_OPTION);

        if (result != 0) return;

        Task task;
        try {
            StringBuilder sampleIdBuf = new StringBuilder();
            for (Sample sample : samples) {
                if (sampleIdBuf.length() > 0) sampleIdBuf.append(",");
                sampleIdBuf.append(sample.getId());
            }

            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
            task = StateMgr.getStateMgr().submitJob("ConsolePurgeAndBlockSample", "Purge And Block Sample", taskParameters);
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
            return;
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Purging and blocking " + samples.size() + " samples";
            }

            @Override
            protected void doStuff() throws Exception {
                setStatus("Executing");
                super.doStuff();
                for (Sample sample : samples) {
                    DomainMgr.getDomainMgr().getModel().invalidate(sample);
                }
                DomainMgr.getDomainMgr().getModel().invalidate(samples);
            }
        };

        taskWorker.executeWithEvents();
    }
}