package org.janelia.workstation.controller.action;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * right-click on workspace in Data Explorer, get info on its sample
 */
@ActionID(
        category = "actions",
        id = "ShowWorkspaceInfo"
)
@ActionRegistration(
        displayName = "#CTL_ShowWorkspaceInfo",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Horta", position = 1520)
})
@NbBundle.Messages("CTL_ShowWorkspaceInfo=Show Sample Info")
public class ShowWorkspaceInfo extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(ShowWorkspaceInfo.class);

    private TmWorkspace workspace;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmWorkspace.class)) {
            workspace = getNodeContext().getSingleObjectOfType(TmWorkspace.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(workspace));
        }
        else {
            workspace = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        TmWorkspace workspace = this.workspace;
        TmSample sample = null;
        try {
        	sample = DomainMgr.getDomainMgr().getModel().getDomainObject(workspace.getSampleRef());
        } catch (Exception e) {
            log.error("Error getting sample "+workspace.getSampleRef(),e);
        }
        String title;
        String message = "Workspace name: " + workspace.getName() + "\n";
        if (sample == null) {
            title = "Error";
            message += "\nCould not retrieve sample entity for this workspace!";
        } else {
            title = "Sample information";
            message += "Sample name: " + sample.getName() + "\n";
            message += "Sample ID: " + sample.getId() + "\n";
            message += "Sample octree path: " + sample.getLargeVolumeOctreeFilepath() + "\n";
            message += "Sample KTX path: " + sample.getLargeVolumeKTXFilepath() + "\n";
            message += "Sample RAW path: " + sample.getAcquisitionFilepath() + "\n";
            message += "RAW compressed: " + sample.hasCompressedAcquisition() + "\n";
        }
        // need to use text area so you can copy the info to clipboard
        JTextArea textarea = new JTextArea(message);
        textarea.setEditable(false);
        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                textarea, title, JOptionPane.PLAIN_MESSAGE);
    }
}
