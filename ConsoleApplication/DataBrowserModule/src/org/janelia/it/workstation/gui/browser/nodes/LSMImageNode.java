package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.io.File;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class LSMImageNode extends DomainObjectNode {
    
    public LSMImageNode(ChildFactory parentChildFactory, LSMImage lsmImage) throws Exception {
        super(parentChildFactory, lsmImage);
        setChildren(Children.LEAF);
    }
    
    private LSMImage getLSMImage() {
        return (LSMImage)getBean();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("images.png").getImage();
    }
    
    @Override
    public String getPrimaryLabel() {
        LSMImage image = getLSMImage();
        if (image != null) {
            File file = new File(image.getLsmFilepath());
            return file.getName();
        }
        return null;
    }
}
