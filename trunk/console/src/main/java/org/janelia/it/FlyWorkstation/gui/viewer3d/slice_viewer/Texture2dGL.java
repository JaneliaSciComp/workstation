package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.TextureCoords;

public class Texture2dGL 
implements PyramidTexture
{
	private int textureId = 0;
	private int width = 0;
	private int height = 0;
	private TextureCoords textureCoords = new TextureCoords(0, 1, 1, 0);
	private boolean linearized = false;

	public Texture2dGL(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	@Override
	public void enable(GL2 gl) {
		gl.glEnable(GL2.GL_TEXTURE_2D);
	}

	@Override
	public void bind(GL2 gl) {
		checkId(gl);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
	}
	
	private void checkId(GL2 gl) {
		if (textureId != 0)
			return;
		int ids[] = {0};
		gl.glGenTextures(1, ids, 0); // count, array, offset
		textureId = ids[0];
	}

	@Override
	public int getTextureId() {
		return textureId;
	}

	@Override
	public void setTexParameteri(GL2 gl, int key, int value) {
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, key, value);
	}

	public boolean isLinearized() {
		return linearized;
	}

	public void setLinearized(boolean linearized) {
		this.linearized = linearized;
	}

	@Override
	public TextureCoords getImageTexCoords() {
		return textureCoords;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void disable(GL2 gl) {
		gl.glDisable(GL2.GL_TEXTURE_2D);
	}

	@Override
	public void destroy(GL2 gl) {
		if (textureId == 0)
			return;
		int ids[] = {textureId};
		gl.glDeleteTextures(1, ids, 0);
		textureId = 0;
	}

	public TextureCoords getTextureCoords() {
		return textureCoords;
	}

	public void setTextureCoords(TextureCoords textureCoords) {
		this.textureCoords = textureCoords;
	}

	@Override
	public int getUsedWidth() {
		TextureCoords tc = getImageTexCoords();
		return (int)Math.round(getWidth() * Math.abs(tc.right() - tc.left()));
	}
}
