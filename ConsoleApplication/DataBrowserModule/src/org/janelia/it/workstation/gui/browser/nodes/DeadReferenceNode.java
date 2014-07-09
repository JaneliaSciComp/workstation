package org.janelia.it.workstation.gui.browser.nodes;

import org.janelia.it.workstation.gui.browser.model.DeadReference;
import java.awt.Image;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;

public class DeadReferenceNode extends DomainObjectNode {
    
    public DeadReferenceNode(ChildFactory parentChildFactory, DeadReference deadReference) throws Exception {
        super(parentChildFactory, Children.LEAF, deadReference);
    }
    
    public DeadReference getDeadReference() {
        return (DeadReference)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return "Missing " + getDeadReference().getType();
    }
        
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("bullet_error.png").getImage();
    }
    
    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canCopy() {
        return false;
    }
}
