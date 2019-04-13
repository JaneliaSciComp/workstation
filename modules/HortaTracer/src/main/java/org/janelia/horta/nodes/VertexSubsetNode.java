package org.janelia.horta.nodes;

import java.awt.Color;
import java.awt.Image;
import java.util.Collection;
import java.util.List;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Christopher Bruns
 */
public class VertexSubsetNode extends AbstractNode
{
    private final VertexSubset vertices;

    VertexSubsetNode(VertexSubset vertices) {
        super(Children.create(new VertexSubsetChildFactory(vertices), true), Lookups.singleton(vertices));
        this.vertices = vertices;
        setDisplayName(vertices.getName());
    }
    
    @Override
    public Image getIcon(int type) {
        // TODO: Icons for islands (0) and links (2)
        if (vertices.getBranchCount() < 2) {
            return ImageUtilities.loadImage("org/janelia/horta/images/VertexTip2.png");
        }
        else {
            return ImageUtilities.loadImage("org/janelia/horta/images/VertexBranch2.png");            
        }
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    public int getSize() {return vertices.getVertices().size();}
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            Property prop;
            // size
            prop = new PropertySupport.Reflection(this, int.class, "getSize", null); 
            prop.setName("size"); 
            set.put(prop); 
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    } // - See more at: https://platform.netbeans.org/tutorials/nbm-nodesapi2.html#sthash.0xrEv8DO.dpuf

    
    private static class VertexSubsetChildFactory extends ChildFactory<NeuronVertex>
    {
        private final VertexSubset vertices;
        
        public VertexSubsetChildFactory(VertexSubset vertices) {
            this.vertices = vertices;
        }

        @Override
        protected boolean createKeys(List<NeuronVertex> toPopulate)
        {
            for (NeuronVertex neuron : vertices.getVertices()) {
                toPopulate.add(neuron);
            }
            return true;
        }
        
        @Override
        protected Node createNodeForKey(NeuronVertex vertex) {
            Collection<NeuronVertex> neighbors = vertices.getNeighborMap().get(vertex);
            return new NeuronVertexNode(vertex, neighbors);
        }
    }
}
