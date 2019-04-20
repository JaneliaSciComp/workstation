package org.janelia.workstation.gui.large_volume_viewer.nodes;

import java.awt.Image;

import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class TmWorkspaceNode extends AbstractDomainObjectNode<TmWorkspace> {

    public TmWorkspaceNode(ChildFactory<?> parentChildFactory, TmWorkspace workspace) {
        super(parentChildFactory, Children.LEAF, workspace);
    }
    
    public TmWorkspace getWorkspace() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getWorkspace().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getWorkspace())) {
            return Icons.getIcon("workspace.png").getImage();
        }
        else {
            return Icons.getIcon("workspace.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
