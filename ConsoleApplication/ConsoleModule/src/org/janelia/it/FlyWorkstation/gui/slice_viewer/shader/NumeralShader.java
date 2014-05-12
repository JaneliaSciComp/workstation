package org.janelia.it.FlyWorkstation.gui.slice_viewer.shader;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.media.opengl.GL2;
import javax.swing.ImageIcon;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.ChannelColorModel;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.ImageColorModel;
import org.janelia.it.FlyWorkstation.gui.util.Icons;

public class NumeralShader extends PassThroughTextureShader 
{

	private ImageColorModel imageColorModel;
	private int numeralTextureId = 0;
	private double micrometersPerPixel = 1.0;
	private int textureWidth = 100;
	private int textureHeight = 100;
	// Allow for rotated pixels, to keep text upright
	private float[][] rotations = { 
			{1,0,0,1},
			{0,1,-1,0},
			{-1,0,0,-1},
			{0,-1,1,0}};
	private int quarterRotations = 0;

	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	@Override
	public String getFragmentShader() {
		return "NumeralFrag.glsl";
	}

	@Override
	public void init(GL2 gl) throws ShaderCreationException {
		super.init(gl);
		ImageIcon numeralIcon = Icons.getIcon("digits_df2.png");
		Image source = numeralIcon.getImage();
		int w = source.getWidth(null);
		int h = source.getHeight(null);
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.drawImage(source, 0, 0, null);
		g2d.dispose();
		// Generate an OpenGL texture handle for numeral distance field
		int ids[] = {0};
		gl.glGenTextures(1, ids, 0); // count, array, offset
		numeralTextureId = ids[0];
		// Produce image pixels
		byte byteArray[] = new byte[w*h];
		ByteBuffer pixels = ByteBuffer.wrap(byteArray);
		pixels.rewind();
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				pixels.put((byte)(image.getRGB(x, y) & 0xff));
			}
		}
		// Upload numeral texture to video card
		gl.glActiveTexture(GL2.GL_TEXTURE1);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, numeralTextureId);
		pixels.rewind();
        gl.glTexImage2D( GL2.GL_TEXTURE_2D,
                0, // mipmap level
                GL2.GL_LUMINANCE,
                w,
                h,
                0, // border
                GL2.GL_LUMINANCE,
                GL2.GL_UNSIGNED_BYTE,
                pixels);
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
        gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
        checkGlError(gl, "Uploading numeral texture");
        //
		gl.glActiveTexture(GL2.GL_TEXTURE0); // revert to normal
	}
	
	@Override
	public void load(GL2 gl) {
		super.load(gl);
		// put numeral texture in texture unit 1
		gl.glActiveTexture(GL2.GL_TEXTURE1);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, numeralTextureId);
		gl.glActiveTexture(GL2.GL_TEXTURE0); // revert to normal
		// set uniform variables
		setUniform(gl, "tileTexture", 0);
		setUniform(gl, "numeralTexture", 1);
		int sc = imageColorModel.getChannelCount();
		assert sc <= 4;
		assert sc >= 1;
		setUniform(gl, "channel_count", sc);
		// NOTE: this assumes channel zero represents max value and bit depth for all
		ChannelColorModel chan = imageColorModel.getChannel(0);
		float nrange = (float)Math.pow(2.0, chan.getBitDepth()) - 1;
		setUniform(gl, "format_max", nrange);
		setUniform(gl, "data_max", (float) chan.getDataMax());
		setUniform(gl, "micrometers_per_pixel", (float) micrometersPerPixel);
		setUniformMatrix2fv(gl, "rotation", false, rotations[quarterRotations]);
		float texturePixels[] = {textureWidth, textureHeight};
		setUniform2fv(gl, "texture_pixels", 1, texturePixels);
		// TODO - srgb_gamma, texture_pixels
		
        checkGlError(gl, "Loading numeral shader");
	}
	
	public void setImageColorModel(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
	}

	public void setMicrometersPerPixel(double micrometersPerPixel) {
		this.micrometersPerPixel = micrometersPerPixel;
	}
	
	public void setQuarterRotations(int quarterRotations) {
		this.quarterRotations = quarterRotations;
	}

	public void setTexturePixels(int width, int height) {
		textureWidth = width;
		textureHeight = height;
	}

}
