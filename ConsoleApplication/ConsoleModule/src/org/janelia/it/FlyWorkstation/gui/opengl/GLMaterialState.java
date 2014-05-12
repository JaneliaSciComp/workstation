package org.janelia.it.FlyWorkstation.gui.opengl;

/**
 * Class to mimic classic fixed-function OpenGL material state, for
 * OpenGL >= 3.1
 * @author brunsc
 *
 */
public class GLMaterialState {
    enum Face {
        FRONT,
        BACK,
        FRONT_AND_BACK,
    }
    
    public Face face = Face.FRONT_AND_BACK;
    public double ambientColor[] = {0.2,0.2,0.2,1};
    public double diffuseColor[] = {0.8,0.8,0.8,1};
    public double specularColor[] = {0,0,0,1};
    public double emissionColor[] = {0,0,0,1};
    public double shininess = 0;
}
