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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "ChangeDataSetAction"
)
@ActionRegistration(
        displayName = "#CTL_ChangeDataSetAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 521)
})
@NbBundle.Messages("CTL_ChangeDataSetAction=Change Data Set")
public class ChangeDataSetAction extends BaseContextualNodeAction {

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
            return "Change Data Set for "+samplesText;
        }
        return "Change Data Set";
    }

    @Override
    public void performAction() {

        Collection<Sample> samples = new ArrayList<>(this.samples);

        ActivityLogHelper.logUserAction("ChangeDataSetAction.actionPerformed");

        final String newDataSet = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "New data set:\n",
                "Change sample data set", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(newDataSet)) {
            return;
        }

        Task task;
        try {
            StringBuilder sampleIdBuf = new StringBuilder();
            for (Sample sample : samples) {
                if (sampleIdBuf.length() > 0) sampleIdBuf.append(",");
                sampleIdBuf.append(sample.getId());
            }

            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleIdBuf.toString(), null));
            taskParameters.add(new TaskParameter("new data set", newDataSet, null));
            task = StateMgr.getStateMgr().submitJob("PostPipeline_ChangeSamplePrimaryKey", "Change Sample Data Set", taskParameters);
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
            return;
        }

        TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

            @Override
            public String getName() {
                return "Changing data set for " + samples.size() + " samples";
            }

            @Override
            protected void doStuff() throws Exception {
                setStatus("Executing");
                super.doStuff();
                DomainMgr.getDomainMgr().getModel().invalidate(samples);
            }
        };

        taskWorker.executeWithEvents();
    }
}