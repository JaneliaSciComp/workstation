package org.janelia.workstation.browser.actions.context;

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
        category = "Actions",
        id = "OpenInVaa3d3dViewAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInVaa3d3dViewAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 231)
})
@NbBundle.Messages("CTL_OpenInVaa3d3dViewAction=Open With Vaa3D 3D View")
public class OpenInVaa3d3dViewAction extends BaseOpenExternallyAction {

    @Override
    public String getName() {
        return OpenInToolAction.getName(ToolMgr.TOOL_VAA3D, ToolMgr.MODE_VAA3D_3D);
    }

    @Override
    public void performAction() {
        OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_VAA3D, filepath, ToolMgr.MODE_VAA3D_3D);
        action.actionPerformed(null);
    }
}
