package org.janelia.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.nodes.AbstractDomainObjectNode;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * Action which implements the opening of a node in a newly created viewer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class OpenInNewViewerAction extends NodeAction {

    private final static OpenInNewViewerAction singleton = new OpenInNewViewerAction();
    public static OpenInNewViewerAction get() {
        return singleton;
    }

    private final List<AbstractDomainObjectNode> selected = new ArrayList<>();

    private OpenInNewViewerAction() {
    }
    
    @Override
    public String getName() {
        return "  Open In New Viewer";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("OpenInNewViewerAction");
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
