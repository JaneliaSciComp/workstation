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
import org.janelia.workstation.core.model.MappingType;
import org.janelia.workstation.core.workers.SimpleWorker;
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

    protected AbstractDomainObjectNode nodeToLoad;
    protected MappingType mappingTypeToLoad;
    protected DomainObject domainObject;

    @Override
    protected void processContext() {
        this.nodeToLoad = null;
        this.mappingTypeToLoad = null;
        this.domainObject = null;
        if (getNodeContext().isSingleNodeOfType(AbstractDomainObjectNode.class)) {
            this.nodeToLoad = getNodeContext().getSingleNodeOfType(AbstractDomainObjectNode.class);
            setEnabledAndVisible(DomainListViewTopComponent.isSupported(nodeToLoad.getDomainObject()));
        }
        else if (getNodeContext().isSingleObjectOfType(DomainObject.class)) {
            this.domainObject = getNodeContext().getSingleObjectOfType(DomainObject.class);
            this.mappingTypeToLoad = DomainViewerManager.getMappingTypeToLoad(domainObject.getClass());
            setEnabledAndVisible(mappingTypeToLoad!=null && DomainViewerTopComponent.isSupported(mappingTypeToLoad.getClazz()));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        if (mappingTypeToLoad !=null) {
            return "Open " + mappingTypeToLoad.getLabel() + " In Viewer";
        }
        return super.getName();
    }

    @Override
    public void performAction() {
        AbstractDomainObjectNode<?> nodeToLoad = this.nodeToLoad;
        MappingType mappingTypeToLoad = this.mappingTypeToLoad;
        try {
            if (nodeToLoad != null) {
                openInViewer(nodeToLoad);
            }
            else if (mappingTypeToLoad != null) {

                SimpleWorker worker = new SimpleWorker() {

                    private DomainObject objectToLoad;
                    @Override
                    protected void doStuff() throws Exception {
                        this.objectToLoad = DomainViewerManager.getObjectToLoad(domainObject);
                    }

                    @Override
                    protected void hadSuccess() {
                        openInViewer(objectToLoad);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        FrameworkAccess.handleException(error);
                    }
                };

                worker.execute();
            }

        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

    protected void openInViewer(AbstractDomainObjectNode nodeToLoad) {
        ActivityLogHelper.logUserAction("OpenInViewerAction.actionPerformed", nodeToLoad);
        DomainListViewTopComponent viewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor2");
        viewer.loadDomainObjectNode(nodeToLoad, true);
    }

    protected void openInViewer(DomainObject objectToLoad) {
        ActivityLogHelper.logUserAction("OpenInViewerAction.actionPerformed", objectToLoad);
        DomainViewerTopComponent viewer = ViewerUtils.provisionViewer(DomainViewerManager.getInstance(), "editor2");
        viewer.loadDomainObject(objectToLoad, true);
    }
}