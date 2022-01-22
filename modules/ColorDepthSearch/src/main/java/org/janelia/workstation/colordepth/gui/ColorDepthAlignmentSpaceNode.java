package org.janelia.workstation.colordepth;

import java.awt.Image;

import org.janelia.workstation.browser.model.ColorDepthAlignmentSpace;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.nodes.FilterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which shows a single alignment space within a color depth library.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthAlignmentSpaceNode extends FilterNode<ColorDepthAlignmentSpace> {

    private final static Logger log = LoggerFactory.getLogger(ColorDepthAlignmentSpaceNode.class);

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
        return Icons.getIcon("folder_red.png").getImage();
    }

    @Override
    public boolean canDestroy() {
        return false;
    }
}
