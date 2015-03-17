package org.janelia.console.viewerapi.model;

import java.awt.Color;
import org.janelia.console.viewerapi.controller.ChannelColorChangeListener;
import org.janelia.console.viewerapi.controller.ColorListener;
import org.janelia.console.viewerapi.controller.ColorModelListener;

public class ChannelColorModel 
{    
	private Color color;
	private int blackLevel; // All intensities below blackLevel will be black
	private double gamma; // gamma exponent between black and white
	private int whiteLevel; // All intensities above whiteLevel will be saturated
	private int dataMax; // Actual maximum value of data, e.g. 4095 for 12-bit in 16-bit container
	private int bitDepth; // log of maximum storable value, e.g. 8 or 16
    private float combiningConstant = 1.0f; // Constant used in making linear combinations of channels.
	private boolean visible = true;
	private int index; // e.g. first channel red is zero, second green is one, etc.

    private final int NUM_SERIALIZED_ITEMS = 9;
    
    private ColorModelListener colorModelListener;
    private ColorListener colorListener;
    private ChannelColorChangeListener channelColorChangeListener;
	
	public ChannelColorModel(int index, Color color, int bitDepth) {
		this.index = index;
		this.color = color;
		this.bitDepth = bitDepth;
		blackLevel = 0;
		gamma = 1.0;
		whiteLevel = dataMax = (int)(Math.pow(2.0, bitDepth) - 0.9);
	}

    /**
     * @param colorModelListener the colorModelListener to set
     */
    public void setColorModelListener(ColorModelListener colorModelListener) {
        this.colorModelListener = colorModelListener;
    }

    /**
     * @param channelColorChangeListener the channelColorChangeListener to set
     */
    public void setChannelColorChangeListener(ChannelColorChangeListener channelColorChangeListener) {
        this.channelColorChangeListener = channelColorChangeListener;
    }
    
    /**
     * @param colorListener the colorListener to set
     */
    public void setColorListener(ColorListener colorListener) {
        this.colorListener = colorListener;
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
        
        builder.append(":");
        builder.append(getCombiningConstant());

        return builder.toString();
    }

    /**
     * parse a string in the format returned by toString() and
     * apply the values to the channel color model
     */
    public void fromString(String modelString) {
        String [] items = modelString.split(":");

        // should be 8 items, not that we're checking
        int inx = 0;
        setBlackLevel(Integer.parseInt(items[inx++]));
        setGamma(Double.parseDouble(items[inx++]));
        setWhiteLevel(Integer.parseInt(items[inx++]));

        Color savedColor = new Color(
            Integer.parseInt(items[inx++]),
            Integer.parseInt(items[inx++]),
            Integer.parseInt(items[inx++]),
            Integer.parseInt(items[inx++])
            );
        setColor(savedColor);
        
        setVisible(Boolean.parseBoolean(items[inx++]));

        // Make this flexible to avoid failing on serialized data from before adding this value.
        if ( items.length > inx ) {
            setCombiningConstant( Float.parseFloat( items[ inx++ ] ) );
        }

        if ( inx > NUM_SERIALIZED_ITEMS ) {
            throw new IllegalStateException("Need to update the number of serialized items.");
        }
    }

	public int getBitDepth() {
		return bitDepth;
	}

	public int getBlackLevel() {
		return blackLevel;
	}

	public Color getColor() {
		return color;
	}

	public int getDataMax() {
		return dataMax;
	}

	public double getGamma() {
		return gamma;
	}

	public int getIndex() {
		return index;
	}

	public int getWhiteLevel() {
		return whiteLevel;
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
        fireBlackLevelChanged(this.blackLevel);
        fireColorModelChanged();
	}

	public void setColor(Color color) {
		if (this.color.equals(color))
			return;
		this.color = color;
        fireColorChange(this.color);
        fireColorModelChanged();

	}

	public void setDataMax(int dataMax) {
		if (this.dataMax == dataMax)
			return;
		this.dataMax = dataMax;
        fireColorModelChanged();
	}

	public void setGamma(double gamma) {
		if (this.gamma == gamma)
			return;
		this.gamma = gamma;
		// System.out.println("gamma = "+gamma);
        fireGammaChanged(this.gamma);
	}

	public void setVisible(boolean visibility) {
		if (visibility == this.visible)
			return;
		this.visible = visibility;
        fireColorModelChanged();
	}

	public void setWhiteLevel(int whiteLevel) {
		if (whiteLevel == this.whiteLevel)
			return;
		this.whiteLevel = whiteLevel;
		// System.out.println("white level = "+whiteLevel);
        fireWhiteLevelChanged(this.whiteLevel);
	}

    /**
     * Use this when combining channels into a linear combination.
     * 
     * @return tells what to multiply by the channel when summing.
     */
    public float getCombiningConstant() {
        return combiningConstant;
    }

    /**
     * Sets the constant to be used in linear combinations with other channels.
     * At present, only 1, -1 are useful.
     * 
     * @param combiningConstant multiply by this, when summing.
     */
    public void setCombiningConstant(float combiningConstant) {
        this.combiningConstant = combiningConstant;
    }

    /**
     * @return the numSerializedItems
     */
    public int getNumSerializedItems() {
        return NUM_SERIALIZED_ITEMS;
    }

    public void fireColorChange(Color color) {
        if (colorListener != null) {
            colorListener.color(color);
        }
    }
    
    private void fireColorModelChanged() {
        if (colorModelListener != null) {
            colorModelListener.colorModelChanged();
        }
    }
    
    private void fireBlackLevelChanged(Integer blackLevel) {
        if (channelColorChangeListener != null) {
            channelColorChangeListener.blackLevelChanged(blackLevel);
        }
    }

    private void fireWhiteLevelChanged(Integer whiteLevel) {
        if (channelColorChangeListener != null) {
            channelColorChangeListener.whiteLevelChanged(whiteLevel);
        }
    }

    private void fireGammaChanged(Double gamma) {
        if (channelColorChangeListener != null) {
            channelColorChangeListener.gammaChanged(gamma);
        }
    }

}
