package org.janelia.horta.actors;

import java.io.IOException;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.gltools.material.Material;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
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
                loadFromPpm(getClass().getResourceAsStream(
                        "/org/janelia/gltools/material/lightprobe/"
                                + "Office1W165Both.ppm"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
}
