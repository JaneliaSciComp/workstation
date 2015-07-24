package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;

import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A top-level Workspace node in the data graph. Functions as a tree node 
 * for most purposes, but some tree node functionality is limited. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkspaceNode extends TreeNodeNode {
    
    private final static Logger log = LoggerFactory.getLogger(WorkspaceNode.class);
    
    public WorkspaceNode(TreeNodeChildFactory parentChildFactory, Workspace workspace) {
        super(parentChildFactory, workspace);
    }
    
    private WorkspaceNode(TreeNodeChildFactory parentChildFactory, final TreeNodeChildFactory childFactory, Workspace workspace) {
        super(parentChildFactory, childFactory, workspace);
    }
    
    public Workspace getWorkspace() {
        return (Workspace)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getTreeNode().getName();
    }
    
    @Override
    public String getExtraLabel() {
        return "("+getTreeNode().getNumChildren()+")";
    }
    
    @Override
    public Image getIcon(int type) {
        if (!getTreeNode().getOwnerKey().equals(SessionMgr.getSubjectKey())) {
            return Icons.getIcon("folder_blue.png").getImage();
        }
        else {
            return Icons.getIcon("folder.png").getImage();    
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
        actions.add(new CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(new RenameAction());
        return actions.toArray(new Action[0]);
    }
}
