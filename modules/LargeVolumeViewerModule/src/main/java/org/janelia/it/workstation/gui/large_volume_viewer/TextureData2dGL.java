package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.glu.GLU;

import org.janelia.it.jacs.shared.lvv.ChannelBrightnessStats;
import org.janelia.it.jacs.shared.lvv.ImageBrightnessStats;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.workstation.gui.opengl.GLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.util.texture.TextureCoords;

public class TextureData2dGL extends TextureData2d
implements TextureDataI
{
	protected static GLU glu = new GLU();
	private static Logger logger = LoggerFactory.getLogger(TextureData2dGL.class);
	
	// Direct glTexImage argument properties
	private int target = GL2GL3.GL_TEXTURE_2D;
	private int internalFormat = GL2GL3.GL_RGB; // # channels/precision/srgb
	private int format = GL2GL3.GL_RGB; // # channels
	private int type = GL2GL3.GL_BYTE; // precision/byte-order

	// Derived properties
	private boolean linearized = false; // whether srgb corrected to linear in hardware
	private boolean swapBytes = false;

	public TextureData2dGL(TextureData2d textureData2d) {
		this.mipmapLevel=textureData2d.getMipmapLevel();
		setWidth(textureData2d.getWidth());
		setUsedWidth(textureData2d.getUsedWidth());
		this.height=textureData2d.getHeight();
		this.border=textureData2d.getBorder();
		this.pixels=textureData2d.getPixels();
		this.srgb=textureData2d.isSrgb();
		this.bitDepth=textureData2d.getBitDepth();
		this.channelCount=textureData2d.getChannelCount();
		updateTexImageParams();
	}

	@Override
	public PyramidTexture createTexture(GL2GL3 gl) {
		Texture2dGL texture = new Texture2dGL(width, height);
		texture.enable(gl);
		texture.bind(gl);
		pixels.rewind();
		if (swapBytes)
			gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, GL.GL_TRUE);
		else
			gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE);
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
		GLError.checkGlError(gl, "glTexImage2D");
		texture.disable(gl);
		texture.setLinearized(linearized);
		// Invert Y texture coordinates so GL system matches image system.
		// Adjust X texture coordinate in case image was padded to even value
		texture.setTextureCoords(new TextureCoords(0, 1, textureCoordX, 0));
		return texture;
	}

	@Override
	public boolean isLinearized() {
		return linearized;
	}

	public boolean isSwapBytes() {
		return swapBytes;
	}

	public void setSwapBytes(boolean swapBytes) {
		this.swapBytes = swapBytes;
	}
	
	public void setBitDepth(int bitDepth) {
		super.setBitDepth(bitDepth);
		updateTexImageParams();
	}
	
	public void setTarget(int target) {
		this.target = target;
	}

	@Override
	public void setLinearized(boolean isLinearized) {
		linearized = isLinearized;
	}
	
	public void updateTexImageParams() {
		linearized = false;
		if (bitDepth == 8)
			type = GL2GL3.GL_UNSIGNED_BYTE;
		else if (bitDepth == 16)
			type = GL2GL3.GL_UNSIGNED_SHORT;
		else
			System.err.println("Unsupported bit depth "+bitDepth);
		
		switch (channelCount) {
		case 1:
			format = internalFormat = GL2GL3.GL_LUMINANCE;
			if (bitDepth == 16)
				internalFormat = GL2.GL_LUMINANCE16; // TODO GL3
			break;
		case 2:
			format = internalFormat = GL2GL3.GL_LUMINANCE_ALPHA;
			if (bitDepth == 16)
				internalFormat = GL2.GL_LUMINANCE16_ALPHA16; // TODO GL3
			break;
		case 3:
			format = internalFormat = GL2GL3.GL_RGB;
			if (bitDepth == 16)
				internalFormat = GL2GL3.GL_RGB16;
			else if (srgb) {
				internalFormat = GL2GL3.GL_SRGB8;
				linearized = true;
			}
			break;
		case 4:
			format = internalFormat = GL2GL3.GL_RGBA;
			if (bitDepth == 16)
				internalFormat = GL2GL3.GL_RGBA16;
			break;
		default:
			System.err.println("Unsupported number of channels "+channelCount);
		}
	}

	public int getTarget() {
		return target;
	}

	public int getInternalFormat() {
		return internalFormat;
	}

	public int getFormat() {
		return format;
	}

	public int getType() {
		return type;
	}

}
