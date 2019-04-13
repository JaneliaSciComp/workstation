
package org.janelia.gltools.material;

import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.GL3Resource;
import org.janelia.gltools.MeshActor;

/**
 *
 * @author brunsc
 */
public interface Material extends GL3Resource {

    public enum Shading {
        SMOOTH, FLAT, NONE
    };

    int getShaderProgramHandle();
    
    public boolean hasPerFaceAttributes();
    
    public void display(
                GL3 gl, 
                MeshActor mesh, 
                AbstractCamera camera,
                Matrix4 modelViewMatrix);

    void load(GL3 gl, AbstractCamera camera);
    
    void setCullFaces(boolean doCull);
    
    void unload(GL3 gl);
    
    boolean usesNormals();
}
