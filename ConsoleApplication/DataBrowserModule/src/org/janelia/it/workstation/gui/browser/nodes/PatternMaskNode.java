package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.screen.PatternMask;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class PatternMaskNode extends DomainObjectNode {
    
    public PatternMaskNode(ChildFactory parentChildFactory, PatternMask patternMask) throws Exception {
        super(parentChildFactory, Children.LEAF, patternMask);
    }
    
    private PatternMask getPatternMask() {
        return (PatternMask)getDomainObject();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick.png").getImage();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getPatternMask().getName();
    }
}
