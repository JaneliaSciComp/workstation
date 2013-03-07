package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;

public class ChannelColorModel 
{
	private Color color;
	private int blackLevel; // All intensities below blackLevel will be black
	private double gamma; // gamma exponent between black and white
	private int whiteLevel; // All intensities above whiteLevel will be saturated
	private int dataMax; // Actual maximum value of data, e.g. 4095 for 12-bit in 16-bit container
	private int bitDepth; // log of maximum storable value, e.g. 8 or 16
	private boolean visible = true;
	private int index; // e.g. first channel red is zero, second green is one, etc.

	private Signal1<Integer> blackLevelChangedSignal = new Signal1<Integer>();
	private Signal1<Color> colorChangedSignal = new Signal1<Color>();
	private Signal1<Integer> dataMaxChangedSignal = new Signal1<Integer>();
	private Signal1<Double> gammaChangedSignal = new Signal1<Double>();
	private Signal1<Integer> whiteLevelChangedSignal = new Signal1<Integer>();
	private Signal1<Boolean> visibilityChangedSignal = new Signal1<Boolean>();
	
	public ChannelColorModel(int index, Color color, int bitDepth) {
		this.index = index;
		this.color = color;
		this.bitDepth = bitDepth;
		blackLevel = 0;
		gamma = 1.0;
		whiteLevel = dataMax = (int)(Math.pow(2.0, bitDepth) - 0.9);
	}
	
	public int getBitDepth() {
		return bitDepth;
	}

	public int getBlackLevel() {
		return blackLevel;
	}

	public Signal1<Integer> getBlackLevelChangedSignal() {
		return blackLevelChangedSignal;
	}

	public Color getColor() {
		return color;
	}

	public Signal1<Color> getColorChangedSignal() {
		return colorChangedSignal;
	}

	public Signal1<Integer> getDataMaxChangedSignal() {
		return dataMaxChangedSignal;
	}

	public Signal1<Boolean> getVisibilityChangedSignal() {
		return visibilityChangedSignal;
	}

	public int getDataMax() {
		return dataMax;
	}

	public double getGamma() {
		return gamma;
	}

	public Signal1<Double> getGammaChangedSignal() {
		return gammaChangedSignal;
	}

	public int getIndex() {
		return index;
	}

	public int getWhiteLevel() {
		return whiteLevel;
	}

	public Signal1<Integer> getWhiteLevelChangedSignal() {
		return whiteLevelChangedSignal;
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void resetContrast() {
		setBlackLevel(0);
		setGamma(1.0);
		setWhiteLevel(getDataMax());
	}

	public void setBlackLevel(int blackLevel) {
		if (this.blackLevel == blackLevel)
			return;
		this.blackLevel = blackLevel;
		blackLevelChangedSignal.emit(this.blackLevel);
	}

	public void setColor(Color color) {
		if (this.color.equals(color))
			return;
		this.color = color;
		colorChangedSignal.emit(this.color);
	}

	public void setDataMax(int dataMax) {
		if (this.dataMax == dataMax)
			return;
		this.dataMax = dataMax;
		dataMaxChangedSignal.emit(this.dataMax);
	}

	public void setGamma(double gamma) {
		if (this.gamma == gamma)
			return;
		this.gamma = gamma;
		gammaChangedSignal.emit(this.gamma);
	}

	public void setVisible(boolean visibility) {
		if (visibility == this.visible)
			return;
		this.visible = visibility;
		visibilityChangedSignal.emit(this.visible);
	}

	public void setWhiteLevel(int whiteLevel) {
		if (whiteLevel == this.whiteLevel)
			return;
		this.whiteLevel = whiteLevel;
		whiteLevelChangedSignal.emit(this.whiteLevel);
	}

}
