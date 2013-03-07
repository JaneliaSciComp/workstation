package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.shader;

import java.awt.Color;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.ChannelColorModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.ImageColorModel;

public class NumeralShader extends PassThroughTextureShader 
{
	private ImageColorModel imageColorModel;

	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	@Override
	public String getFragmentShader() {
		return "ColorFrag.glsl";
	}

	@Override
	public void load(GL2 gl) {
		super.load(gl);
		setUniform(gl, "tileTexture", 0);
		// TODO - load numeral texture
		setUniform(gl, "numeralTexture", 1);
		int sc = imageColorModel.getChannelCount();
		assert sc <= 4;
		assert sc >= 1;
		setUniform(gl, "channel_count", sc);
		// NOTE: this assumes channel zero represents max value and bit depth for all
		ChannelColorModel chan = imageColorModel.getChannel(0);
		float nrange = (float)Math.pow(2.0, chan.getBitDepth());
		setUniform(gl, "format_max", nrange);
		setUniform(gl, "data_max", chan.getDataMax());
		// TODO - srgb_gamma, texture_pixels, micrometers_per_pixel
	}
	
	public void setImageColorModel(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
	}

}
