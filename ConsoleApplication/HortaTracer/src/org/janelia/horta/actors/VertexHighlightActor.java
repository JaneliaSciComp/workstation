package org.janelia.horta.actors;

import java.io.IOException;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Object3d;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 * Single-vertex actor to highlight one existing neuron vertex currently under mouse cursor
 * @author brunsc
 */

public class VertexHighlightActor extends SpheresActor {

    public VertexHighlightActor(NeuronModel neuron) {
        super(neuron, 
                new VertexHighlightTexture(), 
                new SpheresMaterial.SpheresShader());
        material.manageLightProbeTexture = true;
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) 
    {
        // I'm overriding this display() method for debugging.
        if (! isVisible())
            return;
        // 1) Deep check to see whether this actor is actually going to draw
        int geometrySize = 0;
        for (Object3d child : getChildren()) {
            if (child instanceof MeshActor) {
                MeshActor ma = (MeshActor) child;
                geometrySize += ma.getGeometry().size();
            }
        }
        if (geometrySize < 1)
            return;
        // If we get this far, the actor is probably supposed to draw
        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendEquation(GL3.GL_FUNC_ADD);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL3.GL_DEPTH_TEST);
        super.display(gl, camera, parentModelViewMatrix);
    }
    
    private static class VertexHighlightTexture extends Texture2d {
        public VertexHighlightTexture() {
            try {
                // Light probe with a circle added to diffuse component,
                // for emphasis
                loadFromPpm(getClass().getResourceAsStream(
                        "/org/janelia/gltools/material/lightprobe/"
                                + "circleBoth.ppm"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
}
