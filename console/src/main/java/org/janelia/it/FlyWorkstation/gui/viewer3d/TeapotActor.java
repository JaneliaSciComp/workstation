package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;
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
        glut.glutSolidTeapot(1.0);
        gl.glPopAttrib(); // restore GL_FRONT_FACE
	}

	@Override
	public void init(GL2 gl) {}

	@Override
	public void dispose(GL2 gl) {}
}
