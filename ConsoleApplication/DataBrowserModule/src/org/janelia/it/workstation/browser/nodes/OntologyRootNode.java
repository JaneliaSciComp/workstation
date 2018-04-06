package org.janelia.it.workstation.browser.nodes;

import java.awt.Image;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * The root node of the Ontology Explorer, containing the currently selected Ontology node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyRootNode extends AbstractNode implements RootNode {

    private final OntologyRootNodeChildFactory childFactory;
    
    public OntologyRootNode() {
        this(new OntologyRootNodeChildFactory());
    }
    
    private OntologyRootNode(OntologyRootNodeChildFactory childFactory) {
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
