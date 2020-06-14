package org.janelia.workstation.site.jrc.action.context;

import org.apache.commons.lang3.StringUtils;
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

import javax.swing.*;
import java.util.HashSet;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "ChangeSlideCodeAction"
)
@ActionRegistration(
        displayName = "#CTL_ChangeSlideCodeAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 522)
})
@NbBundle.Messages("CTL_ChangeSlideCodeAction=Change Slide Code")
public class ChangeSlideCodeAction extends BaseContextualNodeAction {

    private Sample sample;

    @Override
    protected void processContext() {
        this.sample = null;
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            this.sample = getNodeContext().getSingleObjectOfType(Sample.class);
            if (sample != null) {
                setVisible(true);
                if (ClientDomainUtils.hasWriteAccess(sample)) {
                    setEnabled(true);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Change Slide Code for Sample";
    }

    @Override
    public void performAction() {

        Sample sample = this.sample;

        ActivityLogHelper.logUserAction("ChangeSlideCodeAction.actionPerformed");

        final String newSlideCode = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "New slide code:\n",
                "Change sample slide code", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(newSlideCode)) {
            return;
        }

        Task task;
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
            taskParameters.add(new TaskParameter("new slide code", newSlideCode, null));
            task = StateMgr.getStateMgr().submitJob("PostPipeline_ChangeSamplePrimaryKey", "Change Sample Slide Code", taskParameters);
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
            return;
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Changing slide code for "+sample.getName();
            }

            @Override
            protected void doStuff() throws Exception {
                setStatus("Executing");
                super.doStuff();
                DomainMgr.getDomainMgr().getModel().invalidate(sample);
            }
        };

        taskWorker.executeWithEvents();
    }
}