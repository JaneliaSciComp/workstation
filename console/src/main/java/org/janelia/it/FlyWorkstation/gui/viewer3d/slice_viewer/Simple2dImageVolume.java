package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * Test implementation for showing 2D images in slice viewer.
 */
public class Simple2dImageVolume implements VolumeImage3d, GLActor 
{
	TextureData textureData;
	Texture texture;
	boolean glIsInitialized = false;
	boolean isSrgbApplied = false; // Whether sRGB color correction is already applied to texture
	int channelCount = 0;
	int maxIntensity = 255;
	
	Simple2dImageVolume(String fileName) {
		try {
			URL url = new File(fileName).toURI().toURL();
			loadURL(url);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Override
	public void display(GL2 gl) {
		if (! glIsInitialized)
			init(gl);
		if (texture == null)
			return;
		texture.enable(gl);
		texture.bind(gl);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		int filter = GL2.GL_LINEAR; // TODO - pixelate at high zoom
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, filter);
		texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, filter);
		TextureCoords tc = texture.getImageTexCoords();
		gl.glBegin(GL2.GL_QUADS);
			// draw quad
	        double z = 0.0;
	        double x0 = 0.0;
	        double x1 = x0 + texture.getWidth();
	        double y0 = 0.0;
	        double y1 = y0 - texture.getHeight(); // y inverted in OpenGL relative to image convention
	        gl.glTexCoord2d(tc.left(), tc.bottom()); gl.glVertex3d(x0, y1, z);
	        gl.glTexCoord2d(tc.right(), tc.bottom()); gl.glVertex3d(x1, y1, z);
	        gl.glTexCoord2d(tc.right(), tc.top()); gl.glVertex3d(x1, y0, z);
	        gl.glTexCoord2d(tc.left(), tc.top()); gl.glVertex3d(x0, y0, z);
		gl.glEnd();
		texture.disable(gl);
	}

	@Override
	public void init(GL2 gl) {
		if (textureData == null)
			return;
		gl.glEnable(GL2.GL_FRAMEBUFFER_SRGB);
		texture = TextureIO.newTexture(gl, textureData);
		glIsInitialized = true;
	}

	@Override
	public void dispose(GL2 gl) {
		if (texture != null) {
			texture.destroy(gl);
			texture = null;
			glIsInitialized = false;
		}
	}

	@Override
	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d box = new BoundingBox3d();
		if (textureData != null) {
			box.setMin(new Vec3(0,0,0));
			double x = textureData.getWidth();
			double y = textureData.getHeight();
			double z = 0.0;
			box.setMax(new Vec3(x, y, z));
		}
		return box;
	}

	@Override
	public double getXResolution() {
		// Treat each pixel as 1.0 micrometers
		return 1.0;
	}

	@Override
	public double getYResolution() {
		// Treat each pixel as 1.0 micrometers
		return 1.0;
	}

	@Override
	public double getZResolution() {
		// Treat each pixel as 1.0 micrometers
		return 1.0;
	}

	@Override
	public int getNumberOfChannels() {
		if (textureData == null)
			return 0;
		return channelCount;
	}

	@Override
	public int getMaximumIntensity() {
		return maxIntensity;
	}

	@Override
	public boolean loadURL(URL url) {
		if (url == null)
			return false;
		try {
			BufferedImage image = ImageIO.read(url);
			ColorModel colorModel = image.getColorModel();
			// NOT getNumColorComponents(), because we count alpha channel as data.
			channelCount = colorModel.getNumComponents();
			int bitDepth = colorModel.getPixelSize() / channelCount;
			if (bitDepth > 8)
				maxIntensity = 65535;
			boolean isSRGB = colorModel.getColorSpace().isCS_sRGB();
			// Determine correct OpenGL texture type, based on bit-depth, number of colors, and srgb
			int internalFormat, pixelFormat;
			if (channelCount == 1) {
				internalFormat = pixelFormat = GL2.GL_LUMINANCE; // default for single gray channel
				if (bitDepth > 8)
					internalFormat = GL2.GL_LUMINANCE16;
			}
			else if (channelCount == 2) {
				internalFormat = pixelFormat = GL2.GL_LUMINANCE_ALPHA;
				if (bitDepth > 8)
					internalFormat = GL2.GL_LUMINANCE16_ALPHA16;
			}
			else if (channelCount == 3) {
				internalFormat = pixelFormat = GL2.GL_RGB;
				if (bitDepth > 8)
					internalFormat = GL2.GL_RGB16;
				else if (isSRGB) {
					internalFormat = GL2.GL_SRGB;
					isSrgbApplied = true;
				}
			}
			else if (channelCount == 4) {
				internalFormat = pixelFormat = GL2.GL_RGB8;
				if (bitDepth > 8)
					internalFormat = GL2.GL_RGB16;				
				else if (isSRGB) {
					internalFormat = GL2.GL_SRGB_ALPHA;
					isSrgbApplied = true;
				}
			}
			else {
				System.out.println("Error: unsupported number of channels: " + channelCount);
				return false;
			}
			textureData = AWTTextureIO.newTextureData(
					SliceViewer.glProfile,
					image,
					internalFormat,
					pixelFormat,
					false); // mipmap?
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
