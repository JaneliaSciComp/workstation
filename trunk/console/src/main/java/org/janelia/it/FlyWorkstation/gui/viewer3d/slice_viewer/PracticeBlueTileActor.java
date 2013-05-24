package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

public class PracticeBlueTileActor 
implements GLActor
{
	@Override
	public void display(GL2 gl) {
		// for initial testing, paint a blue square
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
	public void init(GL2 gl) {
	}

	@Override
	public void dispose(GL2 gl) {
	}
}
