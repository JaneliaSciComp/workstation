package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.util.Vector;

public class ImageColorModel 
{
	private static Color defaultChannelColors[] = {
		Color.red,
		Color.green,
		Color.blue,
		Color.lightGray,
		Color.orange,
		Color.yellow,
		Color.pink,
		Color.magenta,
	};
	
	private Vector<ChannelColorModel> channels;
	private boolean blackSynchronized = true;
	private boolean gammaSynchronized = true;
	private boolean whiteSynchronized = true;

	private QtSignal colorModelChangedSignal = new QtSignal();

	
	public ImageColorModel() {
		init(3, 8); // Default to 3 8-bit channels
	}
	
	private void addChannel(Color color, int bitDepth) {
		int c = channels.size();
		ChannelColorModel channel = new ChannelColorModel(
				c, color, bitDepth);
		channels.add(channel);
		// connect signals
		channel.getBlackLevelChangedSignal().connect(colorModelChangedSignal);
		channel.getColorChangedSignal().connect(colorModelChangedSignal);
		channel.getDataMaxChangedSignal().connect(colorModelChangedSignal);
		channel.getGammaChangedSignal().connect(colorModelChangedSignal);
		channel.getWhiteLevelChangedSignal().connect(colorModelChangedSignal);
		channel.getVisibilityChangedSignal().connect(colorModelChangedSignal);
	}

	public boolean isBlackSynchronized() {
		return blackSynchronized;
	}

	public void setBlackSynchronized(boolean blackSynchronized) {
		this.blackSynchronized = blackSynchronized;
	}

	public boolean isGammaSynchronized() {
		return gammaSynchronized;
	}

	public void setGammaSynchronized(boolean gammaSynchronized) {
		this.gammaSynchronized = gammaSynchronized;
	}

	public boolean isWhiteSynchronized() {
		return whiteSynchronized;
	}

	public void setWhiteSynchronized(boolean whiteSynchronized) {
		this.whiteSynchronized = whiteSynchronized;
	}

	public QtSignal getColorModelChangedSignal() {
		return colorModelChangedSignal;
	}

	public ChannelColorModel getChannel(int channelIndex) {
		return channels.get(channelIndex);
	}
	
	public int getChannelCount() {
		return channels.size();
	}
	
	private void init(int channelCount, int bitDepth) {
		channels.clear();
		for (int c = 0; c < channelCount; ++c) {
			int ix = c % defaultChannelColors.length;
			addChannel(defaultChannelColors[ix], bitDepth);
		}
		resetColors(); // in case 1 or 2 channels
	}
	
	public void resetColors() 
	{
		for (ChannelColorModel channel : channels) {
			channel.resetContrast();
			channel.setVisible(true);
		}
		// Set default colors
		// Show single channel as grayscale
		if (getChannelCount() == 1) {
			getChannel(0).setColor(Color.white);
		}
		else if (getChannelCount() == 2) {
			// magenta/green uses orthogonal primaries and balances brightness
			getChannel(0).setColor(Color.magenta);
			getChannel(1).setColor(Color.green);
		}
		else {
			for (ChannelColorModel channel : channels) {
				int ix = channel.getIndex();
				ix = ix % defaultChannelColors.length; // in case we have so many channels, repeat
				channel.setColor(defaultChannelColors[ix]);
			}
		}
	}

}
