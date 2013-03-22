package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

public interface PyramidTextureData {
	public PyramidTexture createTexture(GL2 gl);

	/* 
	 * Whether opengl texture parameters are set to
	 * convert from sRGB to linear.
	 * Useful when sRGB textures are used with an sRGB
	 * framebuffer, to avoid double sRGB-ing.
	 */
	public boolean isLinearized();
	public void setLinearized(boolean isLinearized);
}
