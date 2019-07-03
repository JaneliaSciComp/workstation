package org.janelia.workstation.common.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import org.janelia.model.domain.interfaces.HasIdentifier;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Action to copy a GUID to the clipboard. Supports any object which implements the HasIdentifier interface.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "CopyGUIDToClipboardAction"
)
@ActionRegistration(
        displayName = "CTL_CopyGUIDToClipboardAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 3, separatorAfter = 4)
})
@NbBundle.Messages("CTL_CopyGUIDToClipboardAction=Copy GUID To Clipboard")
public class CopyGUIDToClipboardAction extends BaseContextualNodeAction {

    private HasIdentifier selectedObject;

    public CopyGUIDToClipboardAction() {
        setVisible(true);
    }

    @Override
    protected void processContext() {
        selectedObject = null;
        if (getNodeContext().isSingleObjectOfType(HasIdentifier.class)) {
            selectedObject = getNodeContext().getSingleObjectOfType(HasIdentifier.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        String value = selectedObject.getId()+"";
        ActivityLogHelper.logUserAction("CopyGUIDToClipboardAction.performAction", value);
        Transferable t = new StringSelection(value);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }
}
