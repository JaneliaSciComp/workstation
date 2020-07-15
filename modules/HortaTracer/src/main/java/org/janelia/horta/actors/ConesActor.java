package org.janelia.horta.actors;

import java.awt.Color;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.GenericObservable;
import org.janelia.console.viewerapi.model.DefaultNeuron;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronEdge;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class ConesActor extends BasicGL3Actor 
{
    private final MeshGeometry meshGeometry;
    private final MeshActor meshActor;
    private final ConesMaterial material;
    private final TmNeuronMetadata neuron;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public ConesActor(
            final TmNeuronMetadata neuron,
            Texture2d lightProbeTexture,
            ShaderProgram conesShader) 
    {
        super(null);
        material = new ConesMaterial(lightProbeTexture, conesShader);
        meshGeometry = new MeshGeometry();
        meshActor = new MeshActor(meshGeometry, material, this);
        this.addChild(meshActor);
        this.neuron = neuron;
        setColor(neuron.getColor());
        setMinPixelRadius(0.8f);
        
        updateGeometry();
        
       /* neuron.getVisibilityChangeObservable().addObserver(new Observer() {
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
        neuron.getVertexUpdatedObservable().addObserver(new NeuronVertexUpdateObserver() {
            @Override
            public void update(GenericObservable<VertexWithNeuron> o, VertexWithNeuron arg)
            {
                updateGeometry();
            }
        });
        neuron.getGeometryChangeObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                updateGeometry();
            }
        });*/
    }
    
    private void updateGeometry() {
        // TODO: more careful updating of nodes
        meshGeometry.clear();
        int vertexIndex = 0;
        // logger.info("Edge count = "+neuron.getEdges().size());
        for (TmNeuronEdge neuronEdge : neuron.getEdges()) {
            TmGeoAnnotation parent = neuronEdge.getParentVertex();
            TmGeoAnnotation neuronVertex = neuronEdge.getChildVertex();
        // for (NeuronVertex neuronVertex : neuron.getVertexes()) {
            // NeuronVertex parent = neuronVertex.getParentVertex();
            if (parent == null) 
                continue; // need an edge, to draw a cone

            Vector3 c1 = new Vector3(neuronVertex.getX(), neuronVertex.getY(), neuronVertex.getZ()); // center of first sphere
            Vector3 c2 = new Vector3(parent.getX(), parent.getY(), parent.getZ()); // center of second sphere
            double r1 = DefaultNeuron.radius;
            double r2 = DefaultNeuron.radius;
            if (neuronVertex.getRadius()!=null)
                r1 = neuronVertex.getRadius();
            if (parent.getRadius()!=null)
                r2 = parent.getRadius();

            // To make cones line up perfectly with the spheres, the ends
            // and radii need to be changed. Either here on the CPU,
            // or in the shader on the GPU. Doing so on the GPU could
            // allow better dynamic changes to the radii.
            // This is a subtle effect that mainly affects cones 
            // connecting spheres of very different radii.
            boolean preModifyRadii = false;
            if (preModifyRadii) {
                // Modify locations and radii, so cone is flush with adjacent spheres
                Vector3 cs1 = c1; // center of first sphere
                Vector3 cs2 = c2; // center of second sphere
                double rs1 = r1;
                double rs2 = r2;
                // Swap so r2 is always the largest
                if (rs2 < rs1) {
                    cs2 = new Vector3(neuronVertex.getX(), neuronVertex.getY(), neuronVertex.getZ()); // center of first sphere
                    cs1 = new Vector3(parent.getX(), parent.getY(), parent.getZ()); // center of second sphere
                    rs2 = neuronVertex.getRadius();
                    rs1 = parent.getRadius();
                }
                double d = (cs2.minus(cs1)).length(); // distance between sphere centers
                // half cone angle, to just touch each sphere
                double sinAlpha = (rs2 - rs1) / d;
                double cosAlpha = Math.sqrt(1 - sinAlpha*sinAlpha);
                // Actual cone terminal radii might be smaller than sphere radii
                r1 = cosAlpha * rs1;
                r2 = cosAlpha * rs2;
                // Cone termini might not lie at sphere centers
                Vector3 aHat = (cs1.minus(cs2)).multiplyScalar((float)(1.0/d));
                Vector3 dC1 = new Vector3(aHat).multiplyScalar((float)(sinAlpha * rs1));
                Vector3 dC2 = new Vector3(aHat).multiplyScalar((float)(sinAlpha * rs2));
                // Cone termini
                c1 = cs1.plus(dC1);
                c2 = cs2.plus(dC2);
            }

            // Insert two vertices and an edge into the Geometry object
            Vertex vertex1 = meshGeometry.addVertex(c1);
            vertex1.setAttribute("radius", (float)r1);

            Vertex vertex2 = meshGeometry.addVertex(c2);
            vertex2.setAttribute("radius", (float)r2);

            // logger.info("Node locations " + c1 + ":" + r1 + ", " + c2 + ":" + r2);
            // logger.info("Creating edge " + c1 + ":" + r1 + ", " + c2 + ":" + r2);

            meshGeometry.addEdge(vertexIndex, vertexIndex+1);

            vertexIndex += 2;
        }
        meshGeometry.notifyObservers(); // especially the Material? 
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        // Propagate any pending structure changes...
        if (neuron != null) {
            //neuron.getVisibilityChangeObservable().notifyObservers();
        }
        if (! isVisible()) return;
        if (neuron != null) {
           // neuron.getColorChangeObservable().notifyObservers();
            //neuron.getGeometryChangeObservable().notifyObservers();
        }
        // if (meshGeometry.size() < 1) return;
        // gl.glDisable(GL3.GL_DEPTH_TEST);
        super.display(gl, camera, parentModelViewMatrix);       
    }

    public final void setColor(Color color) {
        material.setColor(color);
    }

    void setMinPixelRadius(float radius)
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
}
