package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

// TODO - glClear() might not work well with stereo?
public class SolidBackgroundActor implements GLActor {

	private float r, g, b;

	public SolidBackgroundActor(float red, float green, float blue) {
		this.r = red;
		this.g = green;
		this.b = blue;
	}
	
	@Override
	public void display(GL2 gl) {
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return null;
	}

	@Override
	public void init(GL2 gl) {
	    gl.glClearColor(r,g,b,1);
	}

	@Override
	public void dispose(GL2 gl) {}

}
