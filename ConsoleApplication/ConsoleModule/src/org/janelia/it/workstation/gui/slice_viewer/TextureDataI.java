package org.janelia.it.workstation.gui.slice_viewer;

import javax.media.opengl.GL2GL3;

public interface TextureDataI {
	public PyramidTexture createTexture(GL2GL3 gl);

	/* 
	 * Whether opengl texture parameters are set to
	 * convert from sRGB to linear.
	 * Useful when sRGB textures are used with an sRGB
	 * framebuffer, to avoid double sRGB-ing.
	 */
	public boolean isLinearized();
	public void setLinearized(boolean isLinearized);
}
