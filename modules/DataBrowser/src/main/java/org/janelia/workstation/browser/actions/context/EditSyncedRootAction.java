package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.workstation.browser.gui.dialogs.SyncedRootDialog;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
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
        id = "EditSyncedRootAction"
)
@ActionRegistration(
        displayName = "#CTL_EditSyncedRootAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 2505)
})
@NbBundle.Messages("CTL_EditSyncedRootAction=Edit Synchronized Folder...")
public class EditSyncedRootAction extends BaseContextualNodeAction {

    private SyncedRoot syncedRoot;

    @Override
    protected void processContext() {
        this.syncedRoot = null;
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(SyncedRoot.class)) {
            this.syncedRoot = getNodeContext().getSingleObjectOfType(SyncedRoot.class);
        }
        if (syncedRoot != null) {
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(syncedRoot));
        }
    }

    @Override
    public void performAction() {
        SyncedRootDialog syncedRootDialog = new SyncedRootDialog();
        syncedRootDialog.showDialog(syncedRoot);
    }
}