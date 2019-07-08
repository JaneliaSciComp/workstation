package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.DomainViewerManager;
import org.janelia.workstation.browser.gui.components.DomainViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
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
public class OpenInNewViewerAction extends BaseContextualNodeAction {

    private DomainObject objectToLoad;
    private AbstractDomainObjectNode nodeToLoad;

    @Override
    protected void processContext() {
        this.objectToLoad = null;
        this.nodeToLoad = null;
        if (getNodeContext().isSingleNodeOfType(AbstractDomainObjectNode.class)) {
            this.nodeToLoad = getNodeContext().getSingleNodeOfType(AbstractDomainObjectNode.class);
            setEnabledAndVisible(DomainListViewTopComponent.isSupported(nodeToLoad.getDomainObject()));
        }
        else if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            DomainObject domainObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            this.objectToLoad = DomainViewerManager.getObjectToLoad(domainObject);
            setEnabledAndVisible(DomainViewerTopComponent.isSupported(domainObject));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        if (objectToLoad!=null) {
            return "Open " + objectToLoad.getType() + " In New Viewer";
        }
        return super.getName();
    }

    @Override
    public void performAction() {
        // We need to save off the instance objects, because when the viewer is provisioned it may overwrite them
        DomainObject objectToLoad = this.objectToLoad;
        AbstractDomainObjectNode nodeToLoad = this.nodeToLoad;
        try {
            if (nodeToLoad != null) {
                ActivityLogHelper.logUserAction("OpenInNewViewerAction.actionPerformed", nodeToLoad);
                DomainListViewTopComponent viewer = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
                viewer.requestActive();
                viewer.loadDomainObjectNode(nodeToLoad, true);
            }
            else if (objectToLoad != null) {
                ActivityLogHelper.logUserAction("OpenInNewViewerAction.actionPerformed", objectToLoad);
                DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(objectToLoad, true);
            }

        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }
}
