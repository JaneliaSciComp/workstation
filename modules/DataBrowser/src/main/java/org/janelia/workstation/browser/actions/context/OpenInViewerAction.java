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
        id = "OpenInViewerAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInViewerAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 50, separatorBefore = 49)
})
@NbBundle.Messages("CTL_OpenInViewerAction=Open In Viewer")
public class OpenInViewerAction extends BaseContextualNodeAction {

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
            return "Open " + objectToLoad.getType() + " In Viewer";
        }
        return super.getName();
    }

    @Override
    public void performAction() {
        DomainObject objectToLoad = this.objectToLoad;
        AbstractDomainObjectNode nodeToLoad = this.nodeToLoad;
        try {
            if (nodeToLoad != null) {
                ActivityLogHelper.logUserAction("OpenInViewerAction.actionPerformed", nodeToLoad);
                DomainListViewTopComponent viewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");
                viewer.loadDomainObjectNode(nodeToLoad, true);
            }
            else if (objectToLoad != null) {
                ActivityLogHelper.logUserAction("OpenInViewerAction.actionPerformed", objectToLoad);
                DomainViewerTopComponent viewer = ViewerUtils.provisionViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.loadDomainObject(objectToLoad, true);
            }

        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }
}