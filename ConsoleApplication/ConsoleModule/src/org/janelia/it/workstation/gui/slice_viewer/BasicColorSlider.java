package org.janelia.it.workstation.gui.slice_viewer;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;

public class BasicColorSlider extends JSlider {
	private static final long serialVersionUID = 1L;

	protected int channelIndex;
	protected ImageColorModel imageColorModel;
	protected int handleWidthPixels = 23;
	protected int leftMarginPixels = 10;
	protected int rightMarginPixels = 10;
	protected int handleWidthValue = 10;

	public BasicColorSlider() {
		super();
	}

	public BasicColorSlider(int orientation) {
		super(orientation);
	}

	public BasicColorSlider(BoundedRangeModel brm) {
		super(brm);
	}

	public BasicColorSlider(int min, int max) {
		super(min, max);
	}

	public BasicColorSlider(int min, int max, int value) {
		super(min, max, value);
	}

	public BasicColorSlider(int orientation, int min, int max, int value) {
		super(orientation, min, max, value);
	}

	public int getChannelIndex() {
		return channelIndex;
	}

	public void setChannelIndex(int channelIndex) {
		this.channelIndex = channelIndex;
	}

	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	public void setImageColorModel(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
	}

}