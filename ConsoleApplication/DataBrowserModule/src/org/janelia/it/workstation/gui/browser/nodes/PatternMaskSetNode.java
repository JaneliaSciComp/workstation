package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.lang.ref.WeakReference;

import org.janelia.it.jacs.model.domain.screen.ScreenSample;
import org.janelia.it.workstation.gui.browser.model.PatternMaskSet;
import org.janelia.it.workstation.gui.browser.nodes.children.PatternMaskNodeFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class PatternMaskSetNode extends InternalNode<PatternMaskSet> {
    
    private final WeakReference<ScreenSample> screenSampleRef;
    private final WeakReference<PatternMaskSet> patternMaskSetRef;
    
    public PatternMaskSetNode(ChildFactory parentChildFactory, ScreenSample screenSample, PatternMaskSet patternMaskSet) throws Exception {
        super(patternMaskSet);
        this.screenSampleRef = new WeakReference<ScreenSample>(screenSample);
        this.patternMaskSetRef = new WeakReference<PatternMaskSet>(patternMaskSet);
        setChildren(Children.create(new PatternMaskNodeFactory(screenSample, patternMaskSet), true));   
    }
    
    private PatternMaskSet getPatternMaskSet() {
        return (PatternMaskSet)getBean();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick.png").getImage();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getPatternMaskSet().getName();
    }
}
