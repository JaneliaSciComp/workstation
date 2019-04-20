package org.janelia.workstation.browser.nodes;

import java.awt.Image;

import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;

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
}
