package org.janelia.it.workstation.gui.browser.nodes;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * An empty node with a message.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EmptyNode extends AbstractNode {
        
    private final String displayName;
    
    public EmptyNode(String displayName) {
        super(Children.LEAF);
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
