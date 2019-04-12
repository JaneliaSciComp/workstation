package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.it.workstation.browser.nb_action.NewDomainObjectAction;
import org.janelia.it.workstation.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.browser.nb_action.SearchHereAction;
import org.janelia.model.domain.workspace.Workspace;

/**
 * A top-level Workspace node in the data graph. Functions as a tree node 
 * for most purposes, but some tree node functionality is limited. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkspaceNode extends TreeNodeNode {
        
    public WorkspaceNode(Workspace workspace) {
        super(null, workspace);
    }
    
    public Workspace getWorkspace() {
        return (Workspace)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getNode().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getNode())) {
            return Icons.getIcon("folder_user.png").getImage();
        }
        else {
            return Icons.getIcon("folder_blue_user.png").getImage();
        }
    }
    
    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canCopy() {
        return false;
    }

    @Override
    public boolean canDestroy() {
        return false;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new CopyToClipboardAction("Name", getName()));
        actions.add(new CopyToClipboardAction("GUID", getId()+""));
        actions.add(null);
        actions.add(new OpenInViewerAction());
        actions.add(new OpenInNewViewerAction());
        actions.add(null);
        actions.add(new ViewDetailsAction());
        actions.add(new ChangePermissionsAction());
        actions.add(NewDomainObjectAction.get());
        actions.add(null);
        actions.add(SearchHereAction.get());
        return actions.toArray(new Action[0]);
    }
}
