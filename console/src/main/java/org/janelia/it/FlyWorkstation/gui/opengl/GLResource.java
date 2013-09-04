package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL2;

public interface GLResource {
	public void init(GL2 gl);
	public void dispose(GL2 gl);
}
