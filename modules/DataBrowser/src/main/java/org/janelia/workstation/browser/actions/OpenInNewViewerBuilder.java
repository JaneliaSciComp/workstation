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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=51)
public class OpenInNewViewerBuilder implements ContextualActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(OpenInNewViewerBuilder.class);

    private static final OpenInNewViewerAction action = new OpenInNewViewerAction();
    private static final OpenInNewViewerNodeAction nodeAction = new OpenInNewViewerNodeAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return nodeAction;
    }

    private static class OpenInNewViewerAction extends ViewerContextAction {

        private DomainObject domainObject;
        private DomainObject objectToLoad;

        @Override
        public String getName() {
            return "Open " + objectToLoad.getType() + " In New Viewer";
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();
            ContextualActionUtils.setVisible(this, false);
            try {
                if (!viewerContext.isMultiple()) {
                    this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
                    this.objectToLoad = DomainViewerManager.getObjectToLoad(domainObject);
                    boolean supported = DomainViewerTopComponent.isSupported(domainObject);
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
                ActivityLogHelper.logUserAction("OpenInNewViewerBuilder.actionPerformed", domainObject);
                DomainViewerTopComponent viewer = ViewerUtils.createNewViewer(DomainViewerManager.getInstance(), "editor2");
                viewer.requestActive();
                viewer.loadDomainObject(objectToLoad, true);
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
        }
    }

    private static class OpenInNewViewerNodeAction extends NodeAction {

        private final List<AbstractDomainObjectNode> selected = new ArrayList<>();

        @Override
        public String getName() {
            return "Open In New Viewer";
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
            DomainListViewTopComponent viewer = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
            viewer.requestActive();
            viewer.loadDomainObjectNode(node, true);
        }
    }
}
