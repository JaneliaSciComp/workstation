package org.janelia.it.workstation.gui.slice_viewer.shader;

import org.janelia.it.workstation.gui.slice_viewer.ChannelColorModel;
import org.janelia.it.workstation.gui.slice_viewer.ImageColorModel;

import java.awt.Color;

import javax.media.opengl.GL2;

public class SliceColorShader extends PassThroughTextureShader
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
		int sc = imageColorModel.getChannelCount();
		assert sc <= 4;
		setUniform(gl, "channel_count", sc);
		// System.out.println("shader Channel count = "+sc);
		
		float channel_color[] = 
			{0, 0, 0,
			 0, 0, 0,
			 0, 0, 0,
			 0, 0, 0};
		float channel_gamma[] = {1,1,1,1};
		float channel_min[] = {0,0,0,0};
		float channel_scale[] = {1,1,1,1};
		for (int c = 0; c < sc; ++c) {
			int offset = 3 * c;
			ChannelColorModel ccm = imageColorModel.getChannel(c);
			Color col = ccm.getColor();
			channel_color[offset + 0] = col.getRed()/255.0f;
			channel_color[offset + 1] = col.getGreen()/255.0f;
			channel_color[offset + 2] = col.getBlue()/255.0f;
			int b = ccm.getBlackLevel();
			int w = ccm.getWhiteLevel();
			float nrange = (float)Math.pow(2.0, ccm.getBitDepth())-1;
			channel_min[c] = b / nrange;
			float crange = (float)Math.max(1.0, w - b); // avoid divide by zero
			channel_scale[c] = nrange/crange;
			if (! ccm.isVisible()) {
				channel_min[c] = 0f;
				channel_scale[c] = 0f;
			}
			channel_gamma[c] = (float)ccm.getGamma();
			/*
			System.out.println(
					"Channel "+c+", "+channel_min[c]+", "+channel_gamma[c]+", "+channel_scale[c]
			        +", "+channel_color[offset+0]+", "+channel_color[offset+1]+", "+channel_color[offset+2]);
			        */
		}
		setUniform3v(gl, "channel_color", 4, channel_color);
		setUniform4v(gl, "channel_gamma", 1, channel_gamma);
		setUniform4v(gl, "channel_min", 1, channel_min);
		setUniform4v(gl, "channel_scale", 1, channel_scale);
	}
	
	public void setImageColorModel(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
	}

}
