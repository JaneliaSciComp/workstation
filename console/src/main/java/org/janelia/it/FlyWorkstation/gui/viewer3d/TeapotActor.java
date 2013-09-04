package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;

import com.jogamp.opengl.util.gl2.GLUT;

public class TeapotActor implements GLActor 
{
    private GLUT glut = new GLUT();

	@Override
	public void display(GL2 gl) {
		// due to a bug in glutSolidTeapot, triangle vertices are in CW order 
        gl.glPushAttrib(GL2.GL_POLYGON_BIT); // remember current GL_FRONT_FACE indictor
        gl.glFrontFace( GL2.GL_CW ); 
        gl.glColor3f(0.40f, 0.27f, 0.00f);
        gl.glMatrixMode(GL2.GL_MODELVIEW_MATRIX);
        gl.glPushMatrix();
        gl.glRotated(180, 1, 0, 0); // Flip teapot to match Y-down convention
        glut.glutSolidTeapot(1.0);
        gl.glPopMatrix();
        gl.glPopAttrib(); // restore GL_FRONT_FACE
	}

	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d result = new BoundingBox3d();
		result.include(new Vec3(1,1,1));
		result.include(new Vec3(-1,-1,-1));
		return result;
	}
	
	@Override
	public void init(GL2 gl) {}

	@Override
	public void dispose(GL2 gl) {}
}
