package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * Not really an action, just the top label on the popup menu.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class PopupLabelAction extends NodeAction {

    private final static PopupLabelAction singleton = new PopupLabelAction();
    public static PopupLabelAction get() {
        return singleton;
    }
    
    private final List<Node> selected = new ArrayList<>();
    
    private PopupLabelAction() {
    }
    
    @Override
    public String getName() {
        DomainObjectNode doNode = (DomainObjectNode)selected.get(0);
        return selected.size()>1 ? "(Multiple selected)" : doNode.getDisplayName();
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("PopupLabelAction");
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            selected.add(node);
        }
        return false;
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
    }
}
