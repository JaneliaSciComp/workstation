package org.janelia.console.viewerapi.color_slider;


import java.awt.Color;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.console.viewerapi.controller.ColorModelListener;

public class UglyColorSlider extends JSlider 
{
	private static final long serialVersionUID = 1L;

	protected int channelIndex;
	protected ImageColorModel imageColorModel;
	private ColorChannelRangeModel rangeModel = new ColorChannelRangeModel();
	private Color whiteColor = Color.white;
	private UglyColorSliderUI ui;

	private boolean updatingFromModel = false; // flag to prevent recursion

	public UglyColorSlider(int channelIndex, ImageColorModel imageColorModel) 
	{
		setModel(rangeModel);        
		this.channelIndex = channelIndex;
		this.imageColorModel = imageColorModel;
		updateSliderValuesFromColorModel();
		imageColorModel.addColorModelListener(new ColorModelListener() {
            @Override
            public void colorModelChanged() {
                updateSliderValuesFromColorModel();
            }            
        });
		rangeModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateColorModelFromSliderValues();
			}
		});
		initSlider();
	}

	public int getBlackLevel() {
		if (rangeModel == null)
    		return getValue();
        else
            return rangeModel.getBlackLevel();
	}

	public int getGrayLevel() {
		if (rangeModel == null)
			return getValue() + getExtent()/2;
		else
			return rangeModel.getGrayLevel();
	}

	public int getWhiteLevel() {
        if (rangeModel == null)
            return getValue() + getExtent();
        else
            return rangeModel.getWhiteLevel();
	}

	private void initSlider() {
		setOrientation(HORIZONTAL);
	}

	public void setBlackLevel(int level) {
        rangeModel.setBlackLevel(level);
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
        setValue(ccm.getBlackLevel());
		// convert gamma to gray level
		// first compute ratio between white and black
		float grayLevel = (float)Math.pow(0.5, 1.0/ccm.getGamma());
		grayLevel = getBlackLevel() + grayLevel*(getWhiteLevel() - getBlackLevel());
		setGrayLevel(Math.round(grayLevel));
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
