package org.janelia.workstation.ndviewer;


import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
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
        id = "OpenInBigDataViewerAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInBigDataViewerAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 1000)
})
@NbBundle.Messages("CTL_OpenInBigDataViewerAction=Open in Big Data Viewer")
public class OpenInBigDataViewerAction extends BaseContextualNodeAction {

    protected AbstractDomainObjectNode nodeToLoad;
    protected DomainObject domainObject;


    @Override
    protected void processContext() {
        this.nodeToLoad = null;
        this.domainObject = null;
        if (getNodeContext().isSingleNodeOfType(AbstractDomainObjectNode.class)) {
            this.nodeToLoad = getNodeContext().getSingleNodeOfType(AbstractDomainObjectNode.class);
            this.domainObject = nodeToLoad.getDomainObject();
            setEnabledAndVisible(BigDataViewerTopComponent.isSupported(nodeToLoad.getDomainObject()));
        } else if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            setEnabledAndVisible(BigDataViewerTopComponent.isSupported(domainObject));
        } else {
            setEnabledAndVisible(false);
        }
    }


    @Override
    public void performAction() {
        BigDataViewerTopComponent viewer = ViewerUtils.provisionViewer(BigDataViewerManager.getInstance(), "editor");
        viewer.loadDomainObject(this.domainObject);
    }
}

