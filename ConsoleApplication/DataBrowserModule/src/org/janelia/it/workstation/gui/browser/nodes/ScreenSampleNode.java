package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.workstation.gui.browser.nodes.children.PatternMaskSetNodeFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenSampleNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(ScreenSampleNode.class);
    
    public ScreenSampleNode(ChildFactory parentChildFactory, ScreenSample screenSample) throws Exception {
        super(parentChildFactory, screenSample);
        setChildren(Children.create(new PatternMaskSetNodeFactory(screenSample), true));
    }
    
    private ScreenSample getScreenSample() {
        return (ScreenSample)getBean();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getScreenSample().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("beaker.png").getImage();
    }
}
