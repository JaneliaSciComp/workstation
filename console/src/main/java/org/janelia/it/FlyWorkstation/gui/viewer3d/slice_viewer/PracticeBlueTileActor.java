package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.geom.Point2D;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

public class PracticeBlueTileActor 
implements GLActor
{
	Vec3 origin = new Vec3();
	Point2D pixelSize = new Point2D.Float(1.0f, 1.0f);
	

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
	public BoundingBox3d getBoundingBox() {
		return null;
	}

	@Override
	public void init(GL2 gl) {
	}

	@Override
	public void dispose(GL2 gl) {
	}
}
