package org.janelia.workstation.site.jrc.action.context;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.dialogs.ConnectDialog;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.*;
import org.janelia.workstation.core.model.ConnectionResult;
import org.janelia.workstation.core.model.DomainModelViewUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.ffmpeg.Frame;
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
        if (AccessManager.getAccessManager().isAdmin() || AccessManager.getAccessManager().isTechnician()) {
            if (getNodeContext().isOnlyObjectsOfType(Sample.class)) {
                for (Sample sample : getNodeContext().getOnlyObjectsOfType(Sample.class)) {
                    setVisible(true);
                    if (ClientDomainUtils.hasWriteAccess(sample)) {
                        samples.add(sample);
                    }
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
                "Change sample data set", JOptionPane.PLAIN_MESSAGE, null, null, samples.iterator().next().getDataSet());
        if (StringUtils.isEmpty(newDataSet)) {
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            private DataSet dataSet;

            @Override
            protected void doStuff() throws Exception {
                dataSet = DomainMgr.getDomainMgr().getModel().getDataSet(newDataSet);
            }

            @Override
            protected void hadSuccess() {

                if (dataSet==null) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                            "Could not find data set.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!ClientDomainUtils.hasWriteAccess(dataSet)) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                            "You do not have write access to target data set "+newDataSet,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                changeDataSet(samples, newDataSet);
            }

            @Override
            protected void hadError(Throwable e) {
                FrameworkAccess.handleException(e);
            }
        };

        worker.execute();
    }

    private void changeDataSet(Collection<Sample> samples, String newDataSet) {

        int result = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(),
                "Are you sure you want to change the data set for these samples to "+newDataSet+"?\n" +
                        "This will affect all the images in these sample in both the Workstation and SAGE, " +
                        "and may also move files on disk, if the owner of the sample changes.",
                "Are you sure?", JOptionPane.OK_CANCEL_OPTION);
        if (result != 0) return;

        for (Sample sample : samples) {
            Task task;
            try {
                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
                taskParameters.add(new TaskParameter("new data set", newDataSet, null));
                task = StateMgr.getStateMgr().submitJob("PostPipeline_ChangeSamplePrimaryKey", "Change Sample Data Set", taskParameters);

                TaskMonitoringWorker taskWorker = new TaskMonitoringWorker(task.getObjectId()) {

                    @Override
                    public String getName() {
                        return "Changing data set for " + sample.getName();
                    }

                    @Override
                    protected void doStuff() throws Exception {
                        setStatus("Executing");
                        super.doStuff();
                        DomainMgr.getDomainMgr().getModel().invalidate(sample);
                    }
                };

                taskWorker.setSuccessCallback(() -> {
                    SimpleWorker.runInBackground(() -> DomainMgr.getDomainMgr().getModel().invalidate(sample));
                    return null;
                });

                taskWorker.executeWithEvents();

            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
                return;
            }
        }
    }
}