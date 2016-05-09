package org.janelia.it.workstation.gui.browser.nb_action;

import java.util.List;

import org.openide.nodes.Node;

/**
 * Not really an action, just the disabled top label on a pop-up menu.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class PopupLabelAction extends NodePresenterAction {

    private final static PopupLabelAction singleton = new PopupLabelAction();
    public static PopupLabelAction get() {
        return singleton;
    }

    private PopupLabelAction() {
    }
    
    @Override
    public String getName() {
        List<Node> selected = getSelectedNodes();
        Node node = selected.get(0);
        return selected.size()>1 ? "(Multiple selected)" : node.getDisplayName();
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        super.enable(activatedNodes);
        return false;
    }
}
