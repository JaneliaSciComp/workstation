package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.screen.FlyLine;
import org.janelia.it.workstation.gui.browser.nodes.children.ScreenSampleNodeFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlyLineNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(FlyLineNode.class);
    
    public FlyLineNode(TreeNodeChildFactory parentChildFactory, FlyLine flyLine) throws Exception {
        super(parentChildFactory, Children.create(new ScreenSampleNodeFactory(flyLine), true), flyLine);
    }
    
    private FlyLine getFlyLine() {
        return (FlyLine)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getFlyLine().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("fruit_fly_small_17.png").getImage();
    }
}
