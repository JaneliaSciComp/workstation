package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.workers.TaskMonitoringWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=530)
public class PurgeAndBlockBuilder implements ContextualActionBuilder {

    private static ProcessingBlockAction action = new ProcessingBlockAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class ProcessingBlockAction extends ViewerContextAction {

        private DomainObject domainObject;
        private List<Sample> samples;

        @Override
        public void setup() {

            ViewerContext viewerContext = getViewerContext();
            this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);

            this.samples = new ArrayList<>();
            for (Object obj : viewerContext.getSelectedObjects()) {
                if (obj instanceof Sample) {
                    samples.add((Sample)obj);
                }
            }

            ContextualActionUtils.setVisible(this, false);
            if (!samples.isEmpty()) {
                ContextualActionUtils.setVisible(this, true);
                ContextualActionUtils.setEnabled(this, true);
                for(Sample sample : samples) {
                    if (!ClientDomainUtils.hasWriteAccess(sample)) {
                        ContextualActionUtils.setEnabled(this, false);
                    }
                }
            }
        }

        @Override
        public String getName() {
            String samplesText = getViewerContext().isMultiple()?samples.size()+" Samples":"Sample";
            return "Purge And Block "+samplesText;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            ActivityLogHelper.logUserAction("ProcessingBlockAction.actionPerformed", domainObject);

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
}