package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public abstract class PyramidTextureLoadAdapter 
{
	public static class MissingTileException extends Exception {
		private static final long serialVersionUID = 1L;
	};
	
	public static class TileLoadError extends Exception {
		private static final long serialVersionUID = 1L;
		public TileLoadError(Throwable e) {
			super(e);
		}
		public TileLoadError(String string) {
			super(string);
		}
	};
	
	protected static PyramidTextureData convertToGlFormat(BufferedImage image) 
	throws TileLoadError 
	{
		ColorModel colorModel = image.getColorModel();
		// NOT getNumColorComponents(), because we count alpha channel as data.
		int channelCount = colorModel.getNumComponents();
		int bitDepth = colorModel.getPixelSize() / channelCount;
		boolean isSrgb = colorModel.getColorSpace().isCS_sRGB();
		// Determine correct OpenGL texture type, based on bit-depth, number of colors, and srgb
		int internalFormat, pixelFormat;
		boolean isSrgbApplied = false;
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
			else if (isSrgb) {
				internalFormat = GL2.GL_SRGB;
				isSrgbApplied = true;
			}
		}
		else if (channelCount == 4) {
			internalFormat = pixelFormat = GL2.GL_RGB8;
			if (bitDepth > 8)
				internalFormat = GL2.GL_RGB16;				
			else if (isSrgb) {
				internalFormat = GL2.GL_SRGB_ALPHA;
				isSrgbApplied = true;
			}
		}
		else {
			throw new TileLoadError("Unsupported number of channels");
		}

		PyramidTextureData result = new JoglTextureData(
				AWTTextureIO.newTextureData(
				SliceViewer.glProfile,
				image,
				internalFormat,
				pixelFormat,
				false)); // mipmap
		result.setLinearized(isSrgbApplied);
		return result;
	}

	protected PyramidTileFormat tileFormat = new PyramidTileFormat();

	abstract PyramidTextureData loadToRam(PyramidTileIndex tileIndex)
		throws TileLoadError, MissingTileException;

	public PyramidTileFormat getTileFormat() {
		return tileFormat;
	}

}
