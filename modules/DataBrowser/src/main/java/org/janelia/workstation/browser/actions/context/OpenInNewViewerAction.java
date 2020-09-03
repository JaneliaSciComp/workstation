package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.components.*;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
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
        id = "OpenInNewViewerAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInNewViewerAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 51, separatorBefore = 49)
})
@NbBundle.Messages("CTL_OpenInNewViewerAction=Open in New Viewer")
public class OpenInNewViewerAction extends OpenInViewerAction {

    @Override
    public String getName() {
        if (mappingTypeToLoad !=null) {
            return "Open " + mappingTypeToLoad.getLabel() + " In New Viewer";
        }
        return super.getName();
    }

    protected void openInViewer(AbstractDomainObjectNode nodeToLoad) {
        ActivityLogHelper.logUserAction("OpenInNewViewerAction.actionPerformed", nodeToLoad);
        DomainListViewTopComponent viewer = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor2");
        viewer.requestActive();
        viewer.loadDomainObjectNode(nodeToLoad, true);
    }

    @Override
    protected void openInViewer(DomainObject objectToLoad) {
        ActivityLogHelper.logUserAction("OpenInNewViewerAction.actionPerformed", objectToLoad);
        DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
        viewer.requestActive();
        viewer.loadDomainObject(objectToLoad, true);
    }
}
