package org.janelia.workstation.lm.actions.context;

import org.janelia.workstation.browser.actions.OpenInToolAction;
import org.janelia.workstation.browser.tools.ToolMgr;
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
        id = "OpenInVaa3dTriViewAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInVaa3dTriViewAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 230)
})
@NbBundle.Messages("CTL_OpenInVaa3dTriViewAction=Open With Default App")
public class OpenInVaa3dTriViewAction extends BaseOpenExternallyAction {

    @Override
    public String getName() {
        return OpenInToolAction.getName(ToolMgr.TOOL_VAA3D, null);
    }

    @Override
    public void performAction() {
        String filepath = getFilepath();
        if (filepath == null) return;
        OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_VAA3D, filepath, null);
        action.actionPerformed(null);
    }
}
