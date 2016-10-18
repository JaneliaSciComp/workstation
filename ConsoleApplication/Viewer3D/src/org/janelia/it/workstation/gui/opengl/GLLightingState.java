package org.janelia.it.workstation.gui.opengl;

import java.util.List;
import java.util.Vector;

/**
 * Class to store fixed-function-like lighting state for OpenGL >= 3.1
 * @author brunsc
 *
 */
public class GLLightingState {
    public List<Light> lights = new Vector<Light>();
    
    public static class Light {
        public double position[] = {0,0,0,0};
        public double diffuseColor[] = {1,1,1,1};
        public double ambientColor[] = {0,0,0,1};
        public double specularColor[] = {0,0,0,1};
        // TODO - attenuation, spot light
    }
}
