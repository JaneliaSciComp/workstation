package org.janelia.it.workstation.gui.large_volume_viewer.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class TmWorkspaceNode extends AbstractDomainObjectNode<TmWorkspace> {

    public TmWorkspaceNode(ChildFactory<?> parentChildFactory, TmWorkspace workspace) throws Exception {
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
