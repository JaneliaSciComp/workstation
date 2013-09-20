package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.util.texture.TextureCoords;

public class TextureData2dGL 
implements TextureDataI
{
	protected static GLU glu = new GLU();
	private static Logger logger = LoggerFactory.getLogger(TextureData2dGL.class);
	
	// Direct glTexImage argument properties
	private int target = GL2.GL_TEXTURE_2D;
	private int mipmapLevel = 0;
	private int internalFormat = GL2.GL_RGB; // # channels/precision/srgb
	private int width = 0; // padded to a multiple of 8
	private int usedWidth = 0; // possibly odd original image width
	private int height = 0;
	private int border = 0;
	private int format = GL2.GL_RGB; // # channels
	private int type = GL2.GL_BYTE; // precision/byte-order
	private ByteBuffer pixels = null; // prefer direct buffer; array backed buffer works too

	// Derived properties
	private boolean linearized = false; // whether srgb corrected to linear in hardware
	private boolean srgb; // vs. linear
	private int bitDepth = 8;
	private int channelCount = 3;
	private float textureCoordX = 1.0f;
	private boolean swapBytes = false;

	private void checkGlError(GL2 gl, String message) 
	{
        int errorNum = gl.glGetError();
        if (errorNum == GL2.GL_NO_ERROR)
        		return;
        String errorStr = glu.gluErrorString(errorNum);
        logger.error( "OpenGL Error " + errorNum + ": " + errorStr + ": " + message );	
	}
	
	@Override
	public PyramidTexture createTexture(GL2 gl) {
		Texture2dGL texture = new Texture2dGL(width, height);
		texture.enable(gl);
		texture.bind(gl);
		pixels.rewind();
		if (swapBytes)
			gl.glPixelStorei(GL2.GL_UNPACK_SWAP_BYTES, GL2.GL_TRUE);
		else
			gl.glPixelStorei(GL2.GL_UNPACK_SWAP_BYTES, GL2.GL_FALSE);
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
		checkGlError(gl, "glTexImage2D");
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

	public void loadRenderedImage(RenderedImage image) {
		ColorModel colorModel = image.getColorModel();
		// If input image uses indexed color table, convert to RGB first.
		if (colorModel instanceof IndexColorModel) {
			IndexColorModel indexColorModel = (IndexColorModel) colorModel;
			image = indexColorModel.convertToIntDiscrete(image.getData(), false);
			colorModel = image.getColorModel();
		}
		this.width = this.usedWidth = image.getWidth();
		// pad image to a multiple of 8
		textureCoordX = 1.0f;
		if ((this.width % 8) != 0) {
			int dw = 8 - (this.width % 8);
			this.width += dw;
			textureCoordX = this.usedWidth / (float)this.width;
		}
		this.height = image.getHeight();
		this.srgb = colorModel.getColorSpace().isCS_sRGB();
		this.channelCount = colorModel.getNumComponents();
		this.bitDepth = colorModel.getPixelSize() / this.channelCount;
		// treat indexed image as rgb
		if (this.bitDepth < 8)
			this.bitDepth = 8;
		assert((this.bitDepth == 8) || (this.bitDepth == 16));
		updateTexImageParams();
		int pixelByteCount = this.channelCount * this.bitDepth/8;
		int rowByteCount = pixelByteCount * this.width;
		int imageByteCount = this.height * rowByteCount;
		// Allocate image store buffer, exactly as it will be passed to openGL
		// TODO - Consider sharing a buffer among multiple textures to save allocation time.
		// (there would need to be a separate one for each thread)
		byte byteArray[] = new byte[imageByteCount];
		ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
		byteBuffer.order(ByteOrder.nativeOrder());
		ShortBuffer shortBuffer = byteBuffer.asShortBuffer(); // for 16-bit case
		//
		Raster raster = image.getData();
		int pixelData[] = new int[this.channelCount];
		int padData[] = new int[this.channelCount]; // color for edge padding
		final boolean is16Bit = (this.bitDepth == 16);
		if (is16Bit) {
			for (int y = 0; y < this.height; ++y) {
				// Choose ragged right edge pad color from right
				// edge of used portion of scan line.
				raster.getPixel(this.usedWidth-1, y, padData);
				for (int x = 0; x < this.width; ++x) {
					if (x < this.usedWidth) { // used portion of scan line
						raster.getPixel(x, y, pixelData);
						for (int i : pixelData) {
							shortBuffer.put((short)i);
						}
					} else { // (not zero) pad right edge
						for (int i : padData) {
							shortBuffer.put((short)i);
						}						
					}
				}
			}
		} else { // 8-bit
			for (int y = 0; y < this.height; ++y) {
				raster.getPixel(this.usedWidth-1, y, padData);
				for (int x = 0; x < this.width; ++x) {
					if (x < this.usedWidth) {
						raster.getPixel(x, y, pixelData);
						for (int i : pixelData) {
							byteBuffer.put((byte)i);
						}
					} else { // zero pad right edge
						for (int i : padData) {
							byteBuffer.put((byte)i);
						}
					}
				}
			}			
		}
		pixels = byteBuffer;
		return;
	}
	
	public void releaseMemory() {
		width = height = usedWidth = 0;
		pixels = null;
	}
	
	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
		updateTexImageParams();
	}
	
	public void setTarget(int target) {
		this.target = target;
	}

	public void setWidth(int width) {
		this.width = width;
		if (width != 0)
			textureCoordX = this.usedWidth / (float)this.width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setPixels(ByteBuffer pixels) {
		this.pixels = pixels;
	}

	public void setChannelCount(int channelCount) {
		this.channelCount = channelCount;
	}

	@Override
	public void setLinearized(boolean isLinearized) {
		linearized = isLinearized;
	}
	
	public void updateTexImageParams() {
		linearized = false;
		if (bitDepth == 8)
			type = GL2.GL_UNSIGNED_BYTE;
		else if (bitDepth == 16)
			type = GL2.GL_UNSIGNED_SHORT;
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

	public int getUsedWidth() {
		return usedWidth;
	}

	public void setUsedWidth(int usedWidth) {
		this.usedWidth = usedWidth;
		if (width != 0)
			textureCoordX = this.usedWidth / (float)this.width;
	}

	public ByteBuffer getPixels() {
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

	public ImageBrightnessStats getBrightnessStats() 
	{
		ByteBuffer bb = getPixels();
		if (bb == null)
			return null;
		if (bb.capacity() < 1)
			return null;
		if (height*width*channelCount < 1)
			return null;
		ImageBrightnessStats result = new ImageBrightnessStats();
		bb.rewind();
		// Initialize channel statistics
		for (int c = 0; c < channelCount; ++c)
			result.add(new ChannelBrightnessStats());
		// Read pixel values
		ShortBuffer buf16 = bb.asShortBuffer(); // ...which might be 16-bit values...
		// First set min/max
		for (int c = 0; c < channelCount; ++c) {
			ChannelBrightnessStats chanStats = result.get(c);
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					int val = 0;
					if (getBitDepth() > 8)
						val = (buf16.get() & 0xffff); // unsigned 16 bit value
					else
						val = (bb.get() & 0xff); // unsigned 8 bit value
					if (val == 0)
						continue; // zero means "no data"
					chanStats.setMax(Math.max(chanStats.getMax(), val));
					chanStats.setMin(Math.min(chanStats.getMin(), val));
				}
			}
		}
		bb.rewind();
		buf16.rewind();
		// Next set histogram, now that min/max are set
		for (int c = 0; c < channelCount; ++c) {
			ChannelBrightnessStats chanStats = result.get(c);
			chanStats.clearHistogram();
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					int val = 0;
					if (getBitDepth() > 8)
						val = (buf16.get() & 0xffff); // unsigned 16 bit value
					else
						val = (bb.get() & 0xff); // unsigned 8 bit value
					if (val == 0)
						continue; // zero means "no data"
					chanStats.updateHistogram(val, 1);
				}
			}
		}
		bb.rewind();
		return result;
	}

}
