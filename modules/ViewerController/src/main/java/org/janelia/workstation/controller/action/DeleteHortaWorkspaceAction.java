package org.janelia.workstation.controller.action;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmMappedNeuron;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.controller.TmViewerManager;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ActionID(
        category = "actions",
        id = "DeleteHortaWorkspaceAction"
)
@ActionRegistration(
        displayName = "#CTL_DeleteHortaWorkspaceAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Horta", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_DeleteHortaWorkspaceAction=Delete Workspace")
public class DeleteHortaWorkspaceAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(DeleteHortaWorkspaceAction.class);
    private DomainObject domainObject;

    public static DeleteHortaWorkspaceAction get() {
        return SystemAction.get(DeleteHortaWorkspaceAction.class);
    }

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmWorkspace.class)) {
            if (AccessManager.getAccessManager().isAdmin() ||
                    domainObject.getOwnerKey().equals(AccessManager.getSubjectKey())) {
                domainObject = getNodeContext().getSingleObjectOfType(TmWorkspace.class);
                setEnabledAndVisible(true);
            }
        }
        else {
            domainObject = null;
            setEnabledAndVisible(false);
        }
    }

    public void setDomainObject(DomainObject obj) {
        domainObject = obj;
    }

    @Override
    public void performAction() {

        DomainObject objectToOpen = domainObject;
        if (objectToOpen==null) {
            log.warn("Action performed with null domain object");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete the selected workspaces?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {

            if (domainObject instanceof TmWorkspace) {
                TmWorkspace workspace = (TmWorkspace) this.domainObject;

                BackgroundWorker deleter = new BackgroundWorker() {
                    @Override
                    public String getName() {
                        return "Deleting TmWorkspaces";
                    }

                    @Override
                    protected void doStuff() {
                        TiledMicroscopeDomainMgr domainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                        List<Long> workspaceIds = new ArrayList<>();
                        workspaceIds.add(workspace.getId());
                        domainMgr.removeWorkspaces(workspaceIds);
                    }

                    @Override
                    protected void hadSuccess() {
                        JOptionPane.showMessageDialog(
                                null,
                                "Selected workspaces deleted successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    }

                    ;

                    @Override
                    protected void hadError(Throwable error) {
                        log.error("Error deleting workspaces", error);
                        JOptionPane.showMessageDialog(
                                null,
                                "Failed to delete workspaces: " + error.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                };
                deleter.executeWithEvents();
            }
        }
    }
}

