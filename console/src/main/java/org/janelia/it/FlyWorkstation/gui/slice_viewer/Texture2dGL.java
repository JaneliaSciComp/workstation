package org.janelia.it.FlyWorkstation.gui.slice_viewer;

// import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

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
	public void enable(GL2GL3 gl) {
		gl.glEnable(GL2GL3.GL_TEXTURE_2D);
	}

	@Override
	public void bind(GL2GL3 gl) {
		checkId(gl);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureId);
	}
	
	private void checkId(GL2GL3 gl) {
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
	public void setTexParameteri(GL2GL3 gl, int key, int value) {
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, key, value);
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
	public void disable(GL2GL3 gl) {
		gl.glDisable(GL2GL3.GL_TEXTURE_2D);
	}

	@Override
	public void destroy(GL2GL3 gl) {
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
