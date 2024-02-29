package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.security.Subject;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.dialog.EditWorkspaceNameDialog;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "WorkspaceSaveAsAction"
)
@ActionRegistration(
        displayName = "Save a copy of the current workspace",
        lazy = true
)
public class WorkspaceSaveAsAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(CreateWorkspaceAction.class);
    public WorkspaceSaveAsAction() {
        super("Save as...");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog();
        final String workspaceName = dialog.showForSample(TmModelManager.getInstance().getCurrentSample());
        String assignOwner = dialog.getAssignOwner();

        if (workspaceName==null) {
            log.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        SimpleWorker creator = new SimpleWorker() {

            private TmWorkspace workspaceCopy;

            @Override
            protected void doStuff() throws Exception {
                workspaceCopy = NeuronManager.getInstance().copyWorkspace(workspace, workspaceName, assignOwner);
            }

            @Override
            protected void hadSuccess() {
                if (workspaceCopy!=null)
                    TmViewerManager.getInstance().loadProject(workspaceCopy);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };

        creator.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Copying workspace...", ""));
        creator.execute();
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        return TmModelManager.getInstance().getCurrentWorkspace()!=null;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
