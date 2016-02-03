package org.janelia.horta.actors;

import java.io.IOException;
import org.janelia.console.viewerapi.model.NeuronModel;
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
