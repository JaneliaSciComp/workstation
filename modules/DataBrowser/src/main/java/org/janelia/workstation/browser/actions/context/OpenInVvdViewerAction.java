package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.workstation.browser.actions.OpenInToolAction;
import org.janelia.workstation.browser.gui.support.SampleUIUtils;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.actions.ViewerContext;
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
public class OpenInVvdViewerAction extends BaseContextualNodeAction {

    private String filepath;

    @Override
    public String getName() {
        return OpenInToolAction.getName(ToolMgr.TOOL_VVD, null);
    }

    @Override
    protected void processContext() {
        this.filepath = null;
        ViewerContext viewerContext = getViewerContext();
        if (viewerContext != null && !viewerContext.isMultiple()) {
            HasFiles fileProvider = SampleUIUtils.getSingle3dResult(viewerContext);
            if (fileProvider != null) {
                this.filepath = DomainUtils.getFilepath(fileProvider, FileType.VisuallyLosslessStack);
            }
        }
        setEnabledAndVisible(filepath != null);
    }

    @Override
    public void performAction() {
        OpenInToolAction action = new OpenInToolAction(ToolMgr.TOOL_VVD, filepath, null);
        action.actionPerformed(null);
    }
}
