package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * The root node of the Data Explorer, containing all the Workspace nodes
 * that the user can read, and other special nodes.
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
        return false;
    }
    
    public void refreshChildren() {
        childFactory.refresh();
    }   
}
