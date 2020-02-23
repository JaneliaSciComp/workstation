package org.janelia.workstation.common.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import org.janelia.model.domain.interfaces.HasName;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Builds action to copy a domain object name to the clipboard.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "CopyNameToClipboardAction"
)
@ActionRegistration(
        displayName = "CTL_CopyNameToClipboardAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions", position = 2, separatorAfter = 5)
})
@NbBundle.Messages("CTL_CopyNameToClipboardAction=Copy Name To Clipboard")
public class CopyNameToClipboardAction extends BaseContextualNodeAction {

    private HasName selectedObject;

    public CopyNameToClipboardAction() {
        setVisible(true);
    }

    @Override
    protected void processContext() {
        selectedObject = null;
        if (getNodeContext().isSingleObjectOfType(HasName.class)) {
            selectedObject = getNodeContext().getSingleObjectOfType(HasName.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        String value = selectedObject.getName();
        ActivityLogHelper.logUserAction("CopyNameToClipboardAction.performAction", value);
        Transferable t = new StringSelection(value);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }
}
