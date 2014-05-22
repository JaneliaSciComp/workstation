package org.janelia.it.workstation.gui.slice_viewer.color_slider;

import javax.swing.DefaultBoundedRangeModel;

public class ColorChannelRangeModel 
extends DefaultBoundedRangeModel 
{
	private static final long serialVersionUID = 1L;
	
	private double gamma;

	private static int grayLevelFromGamma(double gamma, int black, int extent)
	{
		double grayRatio = Math.pow(0.5, 1.0/gamma);
		int grayLevel = black + (int)Math.round(grayRatio*extent);
		return grayLevel;
	}
	
	public ColorChannelRangeModel() {
		super();
	}
	
	public ColorChannelRangeModel(int black, double gamma, int white, int min, int max)
	{
		super(black, white-black, min, max);
		this.gamma = gamma;
	}
	
	public int getBlackLevel() {
		return getValue();
	}

	public double getGamma() {
		return gamma;
	}
	
	public int getGrayLevel() {
		return grayLevelFromGamma(gamma, getBlackLevel(), getExtent());
	}

	public int getWhiteLevel() {
		return getValue() + getExtent();
	}
	
	public void setBlackLevel(int level) {
		setValue(level);
	}
	
	@Override
	public void setExtent(int extent) {
		int newExtent = Math.max(extent, 2);
		super.setExtent(newExtent);
	}
	
	public void setGamma(double gamma) {
		if (this.gamma == gamma)
			return; // no change
		this.gamma = gamma;
		fireStateChanged();
	}
	
	public void setGrayLevel(int level) {
		// clip to black/white range, exclusive
		if (getExtent() < 2) 
			return; // available range is too small
		int grayLevel = level;
		if (grayLevel <= getBlackLevel())
			grayLevel = getBlackLevel() + 1;
		if (grayLevel >= getWhiteLevel())
			grayLevel = getWhiteLevel() - 1;
		int oldGrayLevel = getGrayLevel();
		if (grayLevel == oldGrayLevel)
			return; // no change
		double grayRatio = (grayLevel - getBlackLevel()) / (double)getExtent();
		assert grayRatio > 0;
		assert grayRatio < 1;
		double newGamma = Math.log(0.5)/Math.log(grayRatio);
		assert grayLevel == grayLevelFromGamma(newGamma, getValue(), getExtent());
		setGamma(newGamma);
	}
	
	@Override
	public void setValue(int value) {
		int oldValue = getValue();
		if (oldValue == value) {
			return;
		}
		// Compute new value and extent to maintain upper value.
		int oldExtent = getExtent();
		int newValue = Math.min(Math.max(getMinimum(), value), oldValue + oldExtent - 2);
		int newExtent = oldExtent + oldValue - newValue;
		// Set new value and extent, and fire a single change event.
		setRangeProperties(newValue, newExtent, getMinimum(),
				getMaximum(), getValueIsAdjusting());
	}

	public void setWhiteLevel(int level) {
		// Compute new extent.
		int lowerValue = getValue();
		int newExtent = Math.min(Math.max(0, level - lowerValue), getMaximum() - lowerValue);
		// Set extent to set upper value.
		setExtent(newExtent);
	}	
	
}
