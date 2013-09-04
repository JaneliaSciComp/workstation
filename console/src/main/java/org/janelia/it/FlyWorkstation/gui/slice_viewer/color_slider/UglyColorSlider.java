package org.janelia.it.FlyWorkstation.gui.slice_viewer.color_slider;


import java.awt.Color;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.slice_viewer.ChannelColorModel;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.ImageColorModel;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.Slot;

public class UglyColorSlider extends JSlider 
{
	private static final long serialVersionUID = 1L;

	protected int channelIndex;
	protected ImageColorModel imageColorModel;
	private ColorChannelRangeModel rangeModel = new ColorChannelRangeModel();
	private Color whiteColor = Color.white;
	private UglyColorSliderUI ui;

	private boolean updatingFromModel = false; // flag to prevent recursion

	private Slot updateSliderSlot = new Slot() {
		@Override
		public void execute() {
			updateSliderValuesFromColorModel();
		}
	};

	public UglyColorSlider(int channelIndex, ImageColorModel imageColorModel) 
	{
		setModel(rangeModel);
		this.channelIndex = channelIndex;
		this.imageColorModel = imageColorModel;
		updateSliderValuesFromColorModel();
		imageColorModel.getColorModelChangedSignal().connect(updateSliderSlot);
		rangeModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateColorModelFromSliderValues();
			}
		});
		initSlider();
	}

	public int getBlackLevel() {
		return getValue();
	}

	public int getGrayLevel() {
		if (rangeModel == null)
			return getValue() + getExtent()/2;
		else
			return rangeModel.getGrayLevel();
	}

	public int getWhiteLevel() {
		return getValue() + getExtent();
	}

	private void initSlider() {
		setOrientation(HORIZONTAL);
	}

	public void setBlackLevel(int level) {
		setValue(level);
	}

	public void setGrayLevel(int level) {
		rangeModel.setGrayLevel(level);
	}

	public void setWhiteLevel(int level) {
		rangeModel.setWhiteLevel(level);
	}

	private void updateSliderValuesFromColorModel() {
		if (imageColorModel == null)
			return;
		if (imageColorModel.getChannelCount() <= channelIndex)
			return;
		updatingFromModel = true;
		ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
		setMinimum(0);
		setMaximum(ccm.getDataMax()); // right edge of slider widget
		setBlackLevel(ccm.getBlackLevel());
		setWhiteLevel(ccm.getWhiteLevel());
		// convert gamma to gray level
		// first compute ratio between white and black
		float grayLevel = (float)Math.pow(0.5, 1.0/ccm.getGamma());
		grayLevel = getBlackLevel() + grayLevel*(getWhiteLevel() - getBlackLevel());
		setGrayLevel((int)Math.round(grayLevel));
		// Color
		// setWhiteColor(ccm.getColor());
		updatingFromModel = false;
	}

	public void setWhiteColor(Color color) {
		if (whiteColor == color)
			return;
		whiteColor = color;
		ui.setWhiteColor(whiteColor);
		repaint();
	}

	private void updateColorModelFromSliderValues() {
		if (updatingFromModel) // don't change model while populating slider
			return;
		if (imageColorModel == null)
			return;
		if (imageColorModel.getChannelCount() <= channelIndex)
			return;
		ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
		ccm.setBlackLevel(getBlackLevel());
		ccm.setWhiteLevel(getWhiteLevel());
		ccm.setGamma(rangeModel.getGamma());
	}

	/**
	 * Overrides the superclass method to install the UI delegate to draw three
	 * thumbs.
	 */
	@Override
	public void updateUI() {
		if (ui == null) {
			ui = new UglyColorSliderUI(this);
			addMouseWheelListener(ui.createTrackListener(this));
		}
		setUI(ui);
		// Update UI for slider labels. This must be called after updating the
		// UI of the slider. Refer to JSlider.updateUI().
		updateLabelUIs();
	}

}
