package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.nio.Buffer;

import javax.media.opengl.GL2;

public class TextureData2dGL 
implements PyramidTextureData
{
	// Direct glTexImage argument properties
	private int target = GL2.GL_TEXTURE_2D;
	private int mipmapLevel = 0;
	private int internalFormat = GL2.GL_RGB; // # channels/precision/srgb
	private int width = 0;
	private int height = 0;
	private int border = 0;
	private int format = GL2.GL_RGB; // # channels
	private int type = GL2.GL_BYTE; // precision/byte-order
	private Buffer pixels; // prefer direct buffer; array backed buffer works too

	// Derived properties
	private boolean linearized = false; // whether srgb corrected to linear in hardware
	private boolean srgb; // vs. linear
	private int bitDepth = 8;
	private int channelCount = 3;

	// TODO - method to load a BufferedImage or whatever
	
	@Override
	public PyramidTexture createTexture(GL2 gl) {
		Texture2dGL texture = new Texture2dGL();
		texture.enable(gl);
		texture.bind(gl);
		gl.glTexImage2D(
				target,
				mipmapLevel,
				internalFormat,
				width,
				height,
				border,
				format,
				type,
				pixels);
		texture.disable(gl);
		texture.setLinearized(linearized);
		return texture;
	}

	@Override
	public boolean isLinearized() {
		return linearized;
	}

	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
		updateTexImageParams();
	}
	
	@Override
	public void setLinearized(boolean isLinearized) {
		linearized = isLinearized;
	}
	
	private void updateTexImageParams() {
		linearized = false;
		if (bitDepth == 8)
			type = GL2.GL_BYTE;
		else if (bitDepth == 16)
			type = GL2.GL_SHORT;
		else
			System.err.println("Unsupported bit depth "+bitDepth);
		
		switch (channelCount) {
		case 1:
			format = internalFormat = GL2.GL_LUMINANCE;
			if (bitDepth == 16)
				internalFormat = GL2.GL_LUMINANCE16;
			break;
		case 2:
			format = internalFormat = GL2.GL_LUMINANCE_ALPHA;
			if (bitDepth == 16)
				internalFormat = GL2.GL_LUMINANCE16_ALPHA16;
			break;
		case 3:
			format = internalFormat = GL2.GL_RGB;
			if (bitDepth == 16)
				internalFormat = GL2.GL_RGB16;
			else if (srgb) {
				internalFormat = GL2.GL_SRGB8;
				linearized = true;
			}
			break;
		case 4:
			format = internalFormat = GL2.GL_RGBA;
			if (bitDepth == 16)
				internalFormat = GL2.GL_RGBA16;
			break;
		default:
			System.err.println("Unsupported number of channels "+channelCount);
		}
	}

	public int getTarget() {
		return target;
	}

	public int getMipmapLevel() {
		return mipmapLevel;
	}

	public int getInternalFormat() {
		return internalFormat;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getBorder() {
		return border;
	}

	public int getFormat() {
		return format;
	}

	public int getType() {
		return type;
	}

	public Buffer getPixels() {
		return pixels;
	}

	public boolean isSrgb() {
		return srgb;
	}

	public int getBitDepth() {
		return bitDepth;
	}

	public int getChannelCount() {
		return channelCount;
	}

}
