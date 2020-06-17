package org.janelia.workstation.site.jrc.action.context;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.util.*;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "SyncWithSAGEAction"
)
@ActionRegistration(
        displayName = "#CTL_SyncWithSAGEAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 529)
})
@NbBundle.Messages("CTL_SyncWithSAGEAction=Synchronize Slide Codes with SAGE")
public class SyncWithSAGEAction extends BaseContextualNodeAction {

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
            return "Synchronize Slide Codes with SAGE for "+samplesText;
        }
        return "Synchronize Slide Codes with SAGE";
    }

    @Override
    public void performAction() {

        Collection<Sample> samples = new ArrayList<>(this.samples);

        ActivityLogHelper.logUserAction("DiscoverSlideCodeAction.actionPerformed");

        Set<String> slideCodes = new LinkedHashSet<>();
        for(Sample sample : samples) {
            slideCodes.add(sample.getSlideCode());
        }

        if (slideCodes.size()>100) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "To resynchronize more than 100 slide codes, please contact your system administrator.",
                    "Too many samples selected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                "Are you sure you want to synchronize " + slideCodes.size() + " slide codes with the SAGE database?",
                "Are you sure?", JOptionPane.OK_CANCEL_OPTION);

        if (result != 0) return;

        for(String slideCode : slideCodes) {

            Task task;
            try {
                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter("slide code", slideCode, null));
                task = StateMgr.getStateMgr().submitJob("SlideCodeDiscovery", "Slide Code Discovery", taskParameters);
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
                return;
            }

            TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                @Override
                public String getName() {
                    return "Synchronizing "+slideCode+" with SAGE";
                }

                @Override
                protected void doStuff() throws Exception {
                    setStatus("Executing");
                    super.doStuff();
                }
            };

            taskWorker.setSuccessCallback(() -> {
                SimpleWorker.runInBackground(() -> DomainMgr.getDomainMgr().getModel().invalidate(samples));
                return null;
            });

            taskWorker.executeWithEvents();
        }

    }
}