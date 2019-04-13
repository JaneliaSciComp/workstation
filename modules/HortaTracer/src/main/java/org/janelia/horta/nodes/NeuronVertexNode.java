package org.janelia.horta.nodes;

import java.awt.Color;
import java.awt.Image;
import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronVertexNode extends AbstractNode
{
    private final NeuronVertex vertex;
    private final Collection<NeuronVertex> neighbors;


    NeuronVertexNode(NeuronVertex vertex, Collection<NeuronVertex> neighbors)
    {
        super(Children.create(new NeuronVertexChildFactory(), true), Lookups.singleton(vertex));
        this.vertex = vertex;
        this.neighbors = neighbors;
    }
    
    @Override
    public Image getIcon(int type) {
        // TODO: Icons for islands (0) and links (2)
        if (getSize() < 2) {
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
    
    public int getSize() {
        if (neighbors == null)
            return 0;
        return neighbors.size();
    }
    
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
    }
}
