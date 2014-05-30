package org.janelia.it.workstation.gui.viewer3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.gl2.GLUT;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.opengl.GL3Actor;
import org.janelia.it.workstation.gui.opengl.GLActorContext;
import org.janelia.it.workstation.gui.opengl.GLError;

public class TeapotActor implements GL3Actor
{
    private GLUT glut = new GLUT();

	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d result = new BoundingBox3d();
		result.include(new Vec3(1,1,1));
		result.include(new Vec3(-1,-1,-1));
		return result;
	}
	
    @Override
    public void display(GLActorContext context)
    {
        GL gl = context.getGLAutoDrawable().getGL();
        if (gl.isGL2()) {
            GL2 gl2 = gl.getGL2();
            // due to a bug in glutSolidTeapot, triangle vertices are in CW order 
            gl2.glPushAttrib(GL2.GL_POLYGON_BIT); // remember current GL_FRONT_FACE indicator
            gl.glFrontFace( GL.GL_CW ); 
            gl2.glColor3f(0.40f, 0.27f, 0.00f);
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glPushMatrix();
            gl2.glRotated(180, 1, 0, 0); // Flip teapot to match Y-down convention
            glut.glutSolidTeapot(1.0);
            gl2.glPopMatrix();
            gl2.glPopAttrib(); // restore GL_FRONT_FACE
            
        }
        else {
            // TODO - won't work with GL3...
        }
        GLError.checkGlError(gl, "TeapotActor display");
    }

    @Override
    public void init(GLActorContext context)
    {}

    @Override
    public void dispose(GLActorContext context)
    {}
}
