package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**
 * Base class for node-sensitive actions which are basically sub-menus, 
 * defined by their presenter pop-ups. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class NodePresenterAction extends NodeAction {

    private final List<Node> selected = new ArrayList<>();
        
    @Override
    public String getName() {
        // Implemented by popup presenter
        return "";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }
    
    @Override
    protected void performAction (Node[] activatedNodes) {
        // Implemented by popup presenter
    }

    @Override
    protected boolean asynchronous() {
        // We do our own background processing
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            selected.add(node);
        }
        // Enable state is determined by the popup presenter
        return true;
    }
    
    protected List<Node> getSelectedNodes() {
        return selected;
    }
    
}
