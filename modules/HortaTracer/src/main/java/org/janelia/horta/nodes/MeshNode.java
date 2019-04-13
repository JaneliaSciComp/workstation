package org.janelia.horta.nodes;

import java.awt.Event;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.NeuronTracerTopComponent;
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
public class MeshNode extends AbstractNode
{
    private final MeshActor meshActor;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public MeshNode(final MeshActor meshActor) {
        super(Children.create(new MeshChildFactory(), true), Lookups.singleton(meshActor));
        this.meshActor = meshActor;
        String name = meshActor.getMeshName();
        setDisplayName(name);
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/mesh.png");
    }    
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    @Override
    public Action[] getActions (boolean popup) {
        return new Action[] { 
            new DeleteAction(this.meshActor) 
        };
    }
    
    public boolean isVisible() {
        return meshActor.isVisible();
    }
    
    public void setVisible(boolean visible) {
        if (meshActor.isVisible() == visible)
            return;
        meshActor.setVisible(visible);
        triggerRepaint();
    }
    
    public String getName() {
        return meshActor.getMeshName();
    }
    
    public void setName(String name) {
        NeuronTracerTopComponent hortaTracer = NeuronTracerTopComponent.getInstance();
        hortaTracer.updateObjectMeshName(meshActor.getMeshName(), name);        
        meshActor.setMeshName(name);
        setDisplayName(name);
    }
    
    public void triggerRepaint() {
        // logger.info("NeuronNode repaint triggered");
        // Maybe the parent node would have better access to repainting...
        HortaWorkspaceNode parentNode = (HortaWorkspaceNode)getParentNode();
        if (parentNode != null)
            parentNode.triggerRepaint();
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            Node.Property prop;
            // size
            prop = new PropertySupport.Reflection(this, boolean.class, "isVisible", "setVisible"); 
            prop.setName("visible"); 
            set.put(prop); 
            
            // name
            prop = new PropertySupport.Reflection(this, String.class, "getName", "setName"); 
            prop.setName("name"); 
            set.put(prop); 
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    }
    
    private class DeleteAction extends AbstractAction {
        MeshActor mesh;
        public DeleteAction(MeshActor mesh) {
            putValue (NAME, "DELETE");
            this.mesh = mesh;
        }
        
        @Override
        public void actionPerformed (ActionEvent e) {
            NeuronTracerTopComponent.getInstance().removeMeshActor(mesh);
        }
    }
}
