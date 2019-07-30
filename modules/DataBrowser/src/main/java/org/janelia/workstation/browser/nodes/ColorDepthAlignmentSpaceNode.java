package org.janelia.workstation.browser.nodes;

import java.awt.Image;

import org.janelia.workstation.browser.model.ColorDepthAlignmentSpace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.FilterNode;
import org.janelia.workstation.core.api.ClientDomainUtils;

/**
 * A node which shows a single alignment space within a color depth library.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthAlignmentSpaceNode extends FilterNode<ColorDepthAlignmentSpace> {

    public ColorDepthAlignmentSpaceNode(ColorDepthAlignmentSpace cdas) throws Exception {
        super(null, cdas);
    }

    @Override
    public Long getId() {
        return getColorDepthAlignmentSpace().getId();
    }

    public ColorDepthAlignmentSpace getColorDepthAlignmentSpace() {
        return getDomainObject();
    }

    @Override
    public String getPrimaryLabel() {
        return getColorDepthAlignmentSpace().getAlignmentSpace();
    }

    @Override
    public String getExtraLabel() {
        ColorDepthAlignmentSpace cdas = getColorDepthAlignmentSpace();
        Integer childCount = cdas.getLibrary().getColorDepthCounts().get(cdas.getAlignmentSpace());
        return "("+childCount+")";
    }

    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getFilter())) {
            return Icons.getIcon("search-white-icon.png").getImage();
        }
        else {
            return Icons.getIcon("search-blue-icon.png").getImage();
        }
    }

    @Override
    public boolean canDestroy() {
        return false;
    }
}
