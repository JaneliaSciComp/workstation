package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.DomainViewerManager;
import org.janelia.workstation.browser.gui.components.DomainViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=50)
public class OpenInViewerBuilder implements ContextualActionBuilder {

    private static OpenInViewerAction action = new OpenInViewerAction();
    private static OpenInViewerNodeAction nodeAction = new OpenInViewerNodeAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return nodeAction;
    }

    private static class OpenInViewerAction extends ViewerContextAction {

        private DomainObject domainObject;
        private DomainObject objectToLoad;

        @Override
        public String getName() {
            return "Open " + objectToLoad.getType() + " In Viewer";
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();
            ContextualActionUtils.setVisible(this, false);
            try {
                if (!viewerContext.isMultiple()) {
                    this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
                    this.objectToLoad = DomainViewerManager.getObjectToLoad(domainObject);
                    boolean supported = DomainListViewTopComponent.isSupported(domainObject);
                    ContextualActionUtils.setVisible(this, supported);
                }
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                ActivityLogHelper.logUserAction("OpenInViewerBuilder.actionPerformed", domainObject);
                DomainViewerTopComponent viewer = ViewerUtils.provisionViewer(DomainViewerManager.getInstance(), "editor");
                viewer.loadDomainObject(objectToLoad, true);
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
        }
    }

    private static class OpenInViewerNodeAction extends NodeAction {

        private final List<AbstractDomainObjectNode> selected = new ArrayList<>();

        @Override
        public String getName() {
            return "Open In Viewer";
        }

        @Override
        public HelpCtx getHelpCtx() {
            return new HelpCtx("");
        }

        @Override
        protected boolean asynchronous() {
            return false;
        }

        @Override
        protected boolean enable(org.openide.nodes.Node[] activatedNodes) {
            selected.clear();
            for(org.openide.nodes.Node node : activatedNodes) {
                if (node instanceof AbstractDomainObjectNode) {
                    selected.add((AbstractDomainObjectNode)node);
                }
            }
            if (selected.size()==1) {
                AbstractDomainObjectNode<?> node = selected.get(0);
                return DomainListViewTopComponent.isSupported(node.getDomainObject());
            }
            return false;
        }

        @Override
        protected void performAction(org.openide.nodes.Node[] activatedNodes) {
            if (selected.isEmpty()) return;
            AbstractDomainObjectNode<?> node = selected.get(0);
            DomainListViewTopComponent viewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");
            viewer.loadDomainObjectNode(node, true);
        }
    }
}
