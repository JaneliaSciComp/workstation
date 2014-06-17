package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.BeanNode;

/**
 *
 * @author rokickik
 */
public class DeadReferenceNode extends BeanNode<DeadReference> {
    
    public DeadReferenceNode(DeadReference deadReference) throws Exception {
        super(deadReference);
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("bullet_error.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (getBean() != null) {
            return "<font color='!Label.foreground'>Missing " + getBean().getType() + "</font>";
        } else {
            return null;
        }
    }
    
    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canCopy() {
        return false;
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
}
