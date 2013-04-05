package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

/*
 * Wrapper around jogl Texture class, to help me
 * reimplement similar interface to get 16-bit
 * multichannel textures working correctly.
 */
public class JoglPyramidTexture implements PyramidTexture 
{
	private Texture joglTexture;
	
	public JoglPyramidTexture(Texture newTexture) {
		joglTexture = newTexture;
	}

	@Override
	public void enable(GL2 gl) {
		joglTexture.enable(gl);
	}

	@Override
	public void bind(GL2 gl) {
		joglTexture.bind(gl);
	}

	@Override
	public void setTexParameteri(GL2 gl, int arg1, int arg2) {
		joglTexture.setTexParameteri(gl, arg1, arg2);
	}

	@Override
	public TextureCoords getImageTexCoords() {
		return joglTexture.getImageTexCoords();
	}

	@Override
	public int getWidth() {
		return joglTexture.getWidth();
	}

	@Override
	public int getHeight() {
		return joglTexture.getHeight();
	}

	@Override
	public void disable(GL2 gl) {
		joglTexture.disable(gl);
	}

	@Override
	public void destroy(GL2 gl) {
		joglTexture.destroy(gl);
	}

	@Override
	public int getUsedWidth() {
		TextureCoords tc = getImageTexCoords();
		return (int)Math.round(getWidth() * Math.abs(tc.right() - tc.left()));
	}

}
