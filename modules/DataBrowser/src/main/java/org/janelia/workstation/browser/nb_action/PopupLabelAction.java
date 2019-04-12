package org.janelia.workstation.browser.nb_action;

import java.util.List;

import org.janelia.it.jacs.shared.utils.StringUtils;
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

        if (selected.isEmpty()) {
            return "(Nothing selected)";
        }
        
        if (selected.size()>1) {
            return "(Multiple selected)";
        }
        
        Node node = selected.get(0);
        return StringUtils.abbreviate(node.getDisplayName(), 50);
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        super.enable(activatedNodes);
        return false;
    }
}
