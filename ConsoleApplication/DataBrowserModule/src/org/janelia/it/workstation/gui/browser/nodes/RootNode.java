package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import org.janelia.it.workstation.gui.browser.nodes.children.RootNodeChildFactory;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * The root node of the Data Explorer, containing all the Workspace nodes
 * that the user can read.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RootNode extends AbstractNode {
    
    private final RootNodeChildFactory childFactory;
    
    public RootNode() {
        this(new RootNodeChildFactory());
    }
    
    private RootNode(RootNodeChildFactory childFactory) {
        super(Children.create(childFactory, false));
        this.childFactory = childFactory;
    }
       
    @Override
    public String getDisplayName() {
        // This node should never be displayed
        return "ROOT";
    }
    
    @Override
    public Image getIcon(int type) {
        // This node should never be displayed
        return null;
    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
    public void refreshChildren() {
        childFactory.refresh();
    }   
}
