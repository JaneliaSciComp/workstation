package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.flyem.EMBody;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * Action to copy a FlyEM Body Id to the clipboard. Supports ColorDepthImages (which have body ids)
 * and EMBody objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "CopyBodyIdToClipboardAction"
)
@ActionRegistration(
        displayName = "CTL_CopyBodyIdToClipboardAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 3, separatorAfter = 5)
})
@NbBundle.Messages("CTL_CopyBodyIdToClipboardAction=Copy FlyEM Body Id To Clipboard")
public class CopyBodyIdToClipboardAction extends BaseContextualNodeAction {

    private String bodyId;

    public CopyBodyIdToClipboardAction() {
        setVisible(true);
    }

    @Override
    protected void processContext() {
        setEnabledAndVisible(false);
        if (getNodeContext().isSingleObjectOfType(ColorDepthImage.class)) {
            ColorDepthImage selectedObject = getNodeContext().getSingleObjectOfType(ColorDepthImage.class);
            if (selectedObject.getBodyId() != null) {
                bodyId = selectedObject.getBodyId()+"";
                setEnabledAndVisible(true);
            }
        }

        if (getNodeContext().isSingleObjectOfType(EMBody.class)) {
            EMBody selectedObject = getNodeContext().getSingleObjectOfType(EMBody.class);
            if (selectedObject.getBodyId() != null) {
                bodyId = selectedObject.getBodyId()+"";
                setEnabledAndVisible(true);
            }
        }

    }

    @Override
    public void performAction() {
        ActivityLogHelper.logUserAction("CopyBodyIdToClipboardAction.performAction", bodyId);
        Transferable t = new StringSelection(bodyId);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }
}
