package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.TextureCoords;

public interface PyramidTexture {

	void enable(GL2 gl);

	void bind(GL2 gl);

	void setTexParameteri(GL2 gl, int glTextureWrapS, int glClampToEdge);

	TextureCoords getImageTexCoords();

	int getWidth();

	int getHeight();

	void disable(GL2 gl);

	void destroy(GL2 gl);

}
