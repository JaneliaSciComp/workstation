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
        category = "actions",
        id = "OpenInFijiViewerAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInFijiViewerBuilder",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions", position = 250)
})
@NbBundle.Messages("CTL_OpenInFijiViewerBuilder=Open With Fiji")
public class OpenInFijiViewerAction extends BaseOpenExternallyAction {

    @Override
    public String getName() {
        return OpenInToolAction.getName(ToolMgr.TOOL_FIJI, null);
    }

    @Override
    public void performAction() {
        String filepath = getFilepath();
        if (filepath == null) return;
        OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_FIJI, filepath, null);
        action.actionPerformed(null);
    }
}
