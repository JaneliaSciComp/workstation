package org.janelia.workstation.lm.actions.context;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.actions.OpenInToolAction;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.util.Arrays;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OpenInVvdViewerAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInVvdViewerAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 240)
})
@NbBundle.Messages("CTL_OpenInVvdViewerAction=Open With VVD Viewer")
public class OpenInVvdViewerAction extends BaseOpenExternallyAction {

    @Override
    public String getName() {
        return OpenInToolAction.getName(ToolMgr.TOOL_VVD, null);
    }

    @Override
    protected String getFilepath(HasFiles fileProvider, FileType fileType) {
        // VVD can only open H5J files, not V3DRAW or V3DPBD, so we can't use FirstAvailable3D
        // TODO: this can be better customized to allow TIFF files and other things supported by VVD
        return DomainUtils.getFilepath(fileProvider, FileType.VisuallyLosslessStack);
    }

    @Override
    public void performAction() {
        String filepath = getFilepath();
        if (filepath == null) return;
        if (getSelectedObject() instanceof Sample) {
            Sample sample = (Sample)getSelectedObject();
            String name = sample.getName();
            OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_VVD, filepath, null, Arrays.asList("--desc",name));
            action.actionPerformed(null);
        }
        else {
            OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_VVD, filepath, null);
            action.actionPerformed(null);
        }
    }
}
