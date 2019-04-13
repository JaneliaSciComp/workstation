
package org.janelia.gltools;

import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.CompositeObject3d;

/**
 * Analogous to Three.js CompositeObject3d, and to VTK Actor.
 * A GL3Actor is a VIEW upon a Representation3d MODEL
 * @author brunsc
 * TODO - decompose into Actor/CompositeActor interfaces
 */
public interface GL3Actor extends CompositeObject3d, GL3Resource
{
    /**
     * Render this actor to an openGL context
     * 
     * @param gl The Jogl OpenGL context to render to
     * @param camera Camera object, to provide perspective and view transforms
     * @param parentModelViewMatrix parent model transform. Can be null, in which case the camera view matrix is used.
     */
    void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix);
}
