package org.janelia.horta.actors;

import java.awt.Color;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.model.DefaultNeuron;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexCreationObserver;
import org.janelia.console.viewerapi.model.NeuronVertexDeletionObserver;
import org.janelia.console.viewerapi.model.NeuronVertexUpdateObserver;
import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.texture.Texture2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class SpheresActor extends BasicGL3Actor 
{
    private final MeshGeometry meshGeometry;
    private final MeshActor meshActor;
    protected final SpheresMaterial material;
    private final NeuronModel neuron;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    // Simpler constructor creates its own image and shader resources
    public SpheresActor(NeuronModel neuron) {
        this(neuron, null, new SpheresMaterial.SpheresShader());
    }
    
    // For scaling efficiency, alternate constructor takes shared resources as argument
    public SpheresActor(
            final NeuronModel neuron, 
            Texture2d lightProbeTexture,
            ShaderProgram spheresShader) 
    {
        super(null);
        material = new SpheresMaterial(lightProbeTexture, spheresShader);
        meshGeometry = new MeshGeometry();
        meshActor = new MeshActor(meshGeometry, material, this);
        this.addChild(meshActor);
        this.neuron = neuron;
        setColor(neuron.getColor());
        setMinPixelRadius(0.8f);
        
        updateGeometry();
        
        neuron.getVisibilityChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                setVisible(neuron.isVisible());
            }
        });
        neuron.getColorChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                setColor(neuron.getColor());
            }
        });
        neuron.getGeometryChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                updateGeometry();
            }
        });
        neuron.getVertexCreatedObservable().addObserver(new NeuronVertexCreationObserver() {
            @Override
            public void update(GenericObservable<VertexWithNeuron> o, VertexWithNeuron arg)
            {
                updateGeometry();
            }
        });
        neuron.getVertexUpdatedObservable().addObserver(new NeuronVertexUpdateObserver() {
            @Override
            public void update(GenericObservable<VertexWithNeuron> o, VertexWithNeuron arg)
            {
                updateGeometry();
            }
        });
        neuron.getVertexesRemovedObservable().addObserver(new NeuronVertexDeletionObserver() {
            @Override
            public void update(GenericObservable<VertexCollectionWithNeuron> object, VertexCollectionWithNeuron data) {
                updateGeometry();
            }
        });
    }
    
    private void updateGeometry() {
        // TODO: more careful updating of nodes
        meshGeometry.clear();
        for (NeuronVertex neuronVertex : neuron.getVertexes()) {
            Vertex vertex = meshGeometry.addVertex(neuronVertex.getLocation());
            float radius = DefaultNeuron.radius;
            if (neuronVertex.hasRadius())
                radius = (float) neuronVertex.getRadius();
            vertex.setAttribute("radius", radius);
        }
        meshGeometry.notifyObservers(); // especially the Material?
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        if (neuron == null)
            return;
        
        // Propagate any pending structure changes...
        neuron.getVisibilityChangeObservable().notifyObservers();
        if (! isVisible()) 
            return;        
        
        neuron.getColorChangeObservable().notifyObservers();
        neuron.getGeometryChangeObservable().notifyObservers();
        if (neuron.getVertexes().isEmpty())
            return;

        super.display(gl, camera, parentModelViewMatrix);       
    }

    public final void setColor(Color color) {
        material.setColor(color);
    }

    public void setMinPixelRadius(float radius)
    {
        material.setMinPixelRadius(radius);
    }

    public float[] getColorArray()
    {
        return material.getColorArray();
    }

    public float getMinPixelRadius()
    {
        return material.getMinPixelRadius();
    }

    public NeuronModel getNeuron() {
        return neuron;
    }

}
