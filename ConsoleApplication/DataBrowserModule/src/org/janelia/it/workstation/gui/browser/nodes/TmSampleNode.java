package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.gui.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class TmSampleNode extends DomainObjectNode {

    public TmSampleNode(ChildFactory parentChildFactory, TmSample sample) throws Exception {
        super(parentChildFactory, Children.LEAF, sample);
    }
    
    public TmSample getSample() {
        return (TmSample)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSample().getName();
    }
        
    @Override
    public Image getIcon(int type) {
        if (ClientDomainUtils.isOwner(getSample())) {
            return Icons.getIcon("beaker.png").getImage();
        }
        else {
            return Icons.getIcon("beaker.png").getImage();
        }
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
