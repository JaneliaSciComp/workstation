package org.janelia.it.workstation.gui.large_volume_viewer.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class TmSessionNode extends AbstractDomainObjectNode<TmDirectedSession> {

    public TmSessionNode(ChildFactory<?> parentChildFactory, TmDirectedSession session) throws Exception {
        super(parentChildFactory, Children.LEAF, session);
    }
    
    public TmDirectedSession getSession() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSession().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getSession())) {
            return Icons.getIcon("monitor.png").getImage();
        }
        else {
            return Icons.getIcon("monitor.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
