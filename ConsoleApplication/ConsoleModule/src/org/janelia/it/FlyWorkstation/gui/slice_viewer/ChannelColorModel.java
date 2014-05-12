package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.Color;

import org.janelia.it.FlyWorkstation.gui.animate.DoubleInterpolator;
import org.janelia.it.FlyWorkstation.signal.Signal1;

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

    /**
     * return a string containing the interesting parts of the color model
     * for persisting and later reloading
     */
    public String asString() {
        StringBuilder builder = new StringBuilder();

        builder.append(getBlackLevel());
        builder.append(":");
        builder.append(getGamma());
        builder.append(":");
        builder.append(getWhiteLevel());
        builder.append(":");

        builder.append(getColor().getRed());
        builder.append(":");
        builder.append(getColor().getGreen());
        builder.append(":");
        builder.append(getColor().getBlue());
        builder.append(":");
        builder.append(getColor().getAlpha());
        builder.append(":");

        builder.append(isVisible());

        return builder.toString();
    }

    /**
     * parse a string in the format returned by toString() and
     * apply the values to the channel color model
     */
    public void fromString(String modelString) {
        String [] items = modelString.split(":");

        // should be 8 items, not that we're checking
        setBlackLevel(Integer.parseInt(items[0]));
        setGamma(Double.parseDouble(items[1]));
        setWhiteLevel(Integer.parseInt(items[2]));

        Color savedColor = new Color(
            Integer.parseInt(items[3]),
            Integer.parseInt(items[4]),
            Integer.parseInt(items[5]),
            Integer.parseInt(items[6])
            );
        setColor(savedColor);

        setVisible(Boolean.parseBoolean(items[7]));

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
		// System.out.println("black level = "+blackLevel);
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
		// System.out.println("gamma = "+gamma);
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
		// System.out.println("white level = "+whiteLevel);
		whiteLevelChangedSignal.emit(this.whiteLevel);
	}

}
