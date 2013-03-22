package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/* Wrapper around jogl TextureData class, so I can 
 * reimplement to make 16-bit multichannel textures work
 */
public class JoglTextureData 
implements PyramidTextureData 
{
	
	private TextureData joglTextureData;

	public JoglTextureData(TextureData newTextureData) {
		joglTextureData = newTextureData;
	}

	@Override
	public PyramidTexture createTexture(GL2 gl) {
		return new JoglPyramidTexture(TextureIO.newTexture(gl, joglTextureData));
	}
}
