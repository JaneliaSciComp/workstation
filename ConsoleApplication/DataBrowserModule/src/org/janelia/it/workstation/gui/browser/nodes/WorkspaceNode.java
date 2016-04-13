package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.nb_action.NewDomainObjectAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.util.Icons;

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
        return getTreeNode().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        if (getTreeNode().getOwnerKey().equals(AccessManager.getSubjectKey())) {
            return Icons.getIcon("user-folder-blue-icon.png").getImage();
        }
        else {
            return Icons.getIcon("user-folder-white-icon.png").getImage();
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
        actions.add(new CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(NewDomainObjectAction.get());
        actions.add(new RenameAction());
        return actions.toArray(new Action[0]);
    }
}
