package org.janelia.horta.nodes;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.geometry3d.Box3;
// import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.VantageInterface;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronModelNode extends AbstractNode
{
    private final NeuronModel neuron;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isReadOnly;

    public NeuronModelNode(NeuronModel neuron, boolean isReadOnly) {
        super(Children.create(new NeuronModelChildFactory(neuron), true), Lookups.singleton(neuron));
        setDisplayName(neuron.getName()); //  + " (" + neuron.getVertexes().size() + " vertices)");
        this.neuron = neuron;
        this.isReadOnly = isReadOnly;
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/neuron1.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    private VantageInterface getVantage() {
        Node node = this;
        while (node != null) {
            node = node.getParentNode();
            if (node instanceof HortaWorkspaceNode) {
                return ((HortaWorkspaceNode)node).getVantage();
            }
        }
        return null;
    }
    
    @Override
    public Action[] getActions(boolean popup) {
        List<Action> result = new ArrayList<>();
        // Maybe expose "center on" action
        // 0 - is there a camera to actually center on?
        final VantageInterface vantage = getVantage();
        if ((vantage != null) && neuron.getVertexes().size() > 0) {
            result.add(new CenterOnNeuronAction(neuron, vantage));
        }
        return result.toArray(new Action[result.size()]);
    }
    
    public int getSize() {return neuron.getVertexes().size();}
    public boolean isVisible() {return neuron.isVisible();}
    public void setVisible(boolean visible) {
        if (neuron.isVisible() == visible)
            return;
        neuron.setVisible(visible);
        neuron.getVisibilityChangeObservable().notifyObservers();
        triggerRepaint();
    }
    public Color getColor() {return neuron.getColor();}
    public void setColor(Color color) {
        if (neuron.getColor().equals(color))
            return;
        // logger.info("NeuronNode color set to "+color);
        neuron.setColor(color);
        neuron.getColorChangeObservable().notifyObservers();
        triggerRepaint();
    }
    public void triggerRepaint() {
        // logger.info("NeuronNode repaint triggered");
        // Maybe the parent node would have better access to repainting...
        HortaWorkspaceNode parentNode = (HortaWorkspaceNode)(getParentNode().getParentNode());
        if (parentNode != null)
            parentNode.triggerRepaint();
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            Property prop;
            // visible
            prop = new PropertySupport.Reflection(this, boolean.class, "isVisible", 
                    isReadOnly ? null : "setVisible"); 
            prop.setName("visible");
            set.put(prop); 
            // size
            prop = new PropertySupport.Reflection(this, int.class, "getSize", null); 
            prop.setName("size"); 
            set.put(prop); 
            // color
            prop = new PropertySupport.Reflection(this, Color.class, "getColor", 
                    isReadOnly ? null : "setColor"); 
            prop.setName("color");
            set.put(prop); 
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    } // - See more at: https://platform.netbeans.org/tutorials/nbm-nodesapi2.html#sthash.0xrEv8DO.dpuf


    private static class CenterOnNeuronAction extends AbstractAction
    {
        private final NeuronModel neuron;
        private final VantageInterface vantage;
        
        public CenterOnNeuronAction(NeuronModel neuron, VantageInterface vantage)
        {
            putValue(NAME, "Center on this neuron");
            this.neuron = neuron;
            this.vantage = vantage;
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            // 1 - compute neuron bounding box center point
            Box3 boundingBox = new Box3();
            for (NeuronVertex vertex: neuron.getVertexes())
                boundingBox.include(new Vector3(vertex.getLocation()));
            Vector3 centroid = boundingBox.getCentroid();
            // 2 - find actual vertex closest to that center point
            NeuronVertex closestVertex = neuron.getVertexes().iterator().next();
            float minDistSquared = centroid.distanceSquared(new Vector3(closestVertex.getLocation()));
            for (NeuronVertex vertex: neuron.getVertexes()) {
                float d2 = centroid.distanceSquared(new Vector3(vertex.getLocation()));
                if (d2 < minDistSquared) {
                    minDistSquared = d2;
                    closestVertex = vertex;
                }
            }
            // 3 - center on that vertex
            Vector3 center = new Vector3(closestVertex.getLocation());
            vantage.setFocus(center.getX(), center.getY(), center.getZ());
            // 4 - adjust scale
            float maxScale = 5.0f;
            for (int i = 0; i < 3; ++i) {
                float boxEdge = boundingBox.max.get(i) - boundingBox.min.get(i);
                maxScale = Math.max(maxScale, boxEdge);
            }
            vantage.setSceneUnitsPerViewportHeight(maxScale);

            vantage.notifyObservers();
        }
    }

}
