package org.janelia.it.workstation.gui.slice_viewer;

import javax.media.opengl.GL2GL3;

import com.jogamp.opengl.util.texture.TextureCoords;

public interface PyramidTexture {

	void enable(GL2GL3 gl);

	void bind(GL2GL3 gl);

	void setTexParameteri(GL2GL3 gl, int glTextureWrapS, int glClampToEdge);

	TextureCoords getImageTexCoords();

	int getWidth();

	int getHeight();

	public int getTextureId();

	void disable(GL2GL3 gl);

	void destroy(GL2GL3 gl);

	// Actual texture width might need to be padded to a multiple of 2 or 4 or 8
	// "used width" could be a slightly smaller, possibly odd, value
	int getUsedWidth();

}
