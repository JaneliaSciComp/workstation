package org.janelia.workstation.browser.gui.colordepth;

import java.awt.Image;

import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.workstation.browser.nodes.TreeNodeNode;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.nodes.ChildFactory;

public class ColorDepthMaskNode extends TreeNodeNode {

    public ColorDepthMaskNode(ChildFactory<?> parentChildFactory, ColorDepthMask mask) {
        super(parentChildFactory, mask);
    }
    
    public ColorDepthMask getColorDepthMask() {
        return (ColorDepthMask)getDomainObject();
    }
            
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthMask())) {
            return Icons.getIcon("image.png").getImage();
        }
        else {
            return Icons.getIcon("image.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
