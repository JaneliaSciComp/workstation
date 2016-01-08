package org.janelia.horta.actors;

import java.io.IOException;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 * Single vertex actor representing provisional future neuron vertex centered on imaging density currently under mouse cursor.
 * @author brunsc
 */

public class DensityCursorActor extends SpheresActor {

    public DensityCursorActor(NeuronModel neuron) {
        super(neuron, 
                new DensityHighlightTexture(), 
                new SpheresMaterial.SpheresShader());
        material.manageLightProbeTexture = true;
    }

    private static class DensityHighlightTexture extends Texture2d {
        public DensityHighlightTexture() {
            try {
                // Light probe with plus sign ("+") decoration
                loadFromPpm(getClass().getResourceAsStream(
                        "/org/janelia/gltools/material/lightprobe/"
                                + "plusBoth.ppm"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
}
