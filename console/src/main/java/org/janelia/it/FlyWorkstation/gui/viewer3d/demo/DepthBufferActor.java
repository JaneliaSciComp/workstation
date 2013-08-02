package org.janelia.it.FlyWorkstation.gui.viewer3d.demo;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;

public class DepthBufferActor implements GLActor {

	@Override
	public void display(GL2 gl) {
		gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		return null;
	}

	@Override
	public void init(GL2 gl) {
		gl.glEnable(GL2.GL_DEPTH_TEST);
	}

	@Override
	public void dispose(GL2 gl) {
	}

}
