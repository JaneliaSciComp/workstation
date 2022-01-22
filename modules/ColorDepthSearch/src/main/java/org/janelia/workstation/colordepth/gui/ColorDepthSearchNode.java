package org.janelia.workstation.colordepth.gui;

import java.awt.Image;

import org.janelia.model.domain.gui.cdmip.ColorDepthSearch;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorDepthSearchNode extends AbstractDomainObjectNode<ColorDepthSearch> {

    private static final Logger log = LoggerFactory.getLogger(ColorDepthSearchNode.class);
    
    public ColorDepthSearchNode(ChildFactory<?> parentChildFactory, ColorDepthSearch mask) {
        super(parentChildFactory, Children.LEAF, mask);
    }
    
    public ColorDepthSearch getColorDepthSearch() {
        return getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getColorDepthSearch().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getColorDepthSearch())) {
            return Icons.getIcon("drive_magnify.png").getImage();
        }
        else {
            return Icons.getIcon("drive_magnify.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
