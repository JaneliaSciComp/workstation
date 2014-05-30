package org.janelia.it.workstation.gui.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;

public class PracticeBlueTileActor 
implements GLActor
{
	@Override
	public void display(GLAutoDrawable glDrawable) {
		// for initial testing, paint a blue square
        GL2 gl = glDrawable.getGL().getGL2();
		gl.glColor3d(0.2, 0.2, 1.0);
		gl.glBegin(GL2.GL_QUADS);
			gl.glVertex3d( 0,  0, 0);
			gl.glVertex3d( 0, 10, 0);
			gl.glVertex3d(10, 10, 0);
			gl.glVertex3d(10,  0, 0);
		gl.glEnd();
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		// NOTE - Y coordinate is inverted w.r.t. glVertex3d(...)
		BoundingBox3d result = new BoundingBox3d();
		result.setMin(0, -10, 0);
		result.setMax(10,  0, 0);
		return result;
	}

	@Override
	public void init(GLAutoDrawable glDrawable) {
	}

	@Override
	public void dispose(GLAutoDrawable glDrawable) {
	}
}
