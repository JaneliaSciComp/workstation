package org.janelia.horta.actors;

import java.io.IOException;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author brunsc
 */


public class ParentVertexActor extends SpheresActor {

    public ParentVertexActor(NeuronModel neuron) {
        super(neuron, 
                new ParentVertexTexture(), 
                new SpheresMaterial.SpheresShader());
        material.manageLightProbeTexture = true;
    }

    private static class ParentVertexTexture extends Texture2d {
        public ParentVertexTexture() {
            try {
                loadFromPpm(getClass().getResourceAsStream(
                        "/org/janelia/gltools/material/lightprobe/"
                                + "parentBoth.ppm"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
}
