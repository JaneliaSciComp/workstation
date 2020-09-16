package org.janelia.workstation.controller.action;

import org.janelia.workstation.controller.dialog.EditWorkspaceNameDialog;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Horta",
        id = "CreateWorkspaceAction"
)
@ActionRegistration(
        displayName = "#CTL_CreateWorkspaceAction",
        lazy = false
)
@NbBundle.Messages("CTL_CreateWorkspaceAction=Create New Workspace in Sample")
public class CreateWorkspaceAction extends AbstractAction {
    private TmWorkspace origWorkspace;
    private static final Logger log = LoggerFactory.getLogger(CreateWorkspaceAction.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        createWorkspace();
    }

    public void createWorkspace() {
        TmSample sample = TmModelManager.getInstance().getCurrentSample();
        if (sample == null) {
            FrameworkAccess.handleException(new Throwable("You must load a brain sample entity before creating a workspace!"));
            return;
        }

        // ask the user if they really want a new workspace if one is active
        final boolean existingWorkspace = TmModelManager.getInstance().getCurrentWorkspace() != null;
        if (existingWorkspace) {
            origWorkspace = TmModelManager.getInstance().getCurrentWorkspace();
            int ans = JOptionPane.showConfirmDialog(
                    null,
                    "You already have an active workspace!  Close and create another?",
                    "Workspace exists",
                    JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.NO_OPTION) {
                return;
            }
        }

        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog();
        final String workspaceName = dialog.showForSample(TmModelManager.getInstance().getCurrentSample());

        if (workspaceName == null) {
            log.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        // create it in another thread
        // there is no doubt a better way to get these parameters in:
        final Long finalSampleId = sample.getId();
        SimpleWorker creator = new SimpleWorker() {
            private TmWorkspace workspace;

            @Override
            protected void doStuff() throws Exception {

                log.info("Creating new workspace with name '{}' for {}",workspaceName,finalSampleId);

                // now we can create the workspace
                NeuronManager neuronManager = NeuronManager.getInstance();
                this.workspace = neuronManager.createWorkspace(finalSampleId, workspaceName);
                log.info("Created workspace with id={}",workspace.getId());

                // Reuse the existing color model
                if (existingWorkspace) {
                    workspace.setColorModel(origWorkspace.getColorModel());
                    TiledMicroscopeDomainMgr.getDomainMgr().save(workspace);
                    log.info("Copied existing color model");
                }
            }

            @Override
            protected void hadSuccess() {
                TmViewerManager.getInstance().loadProject(workspace);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        creator.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Creating new workspace...", ""));
        creator.execute();
    }

}
