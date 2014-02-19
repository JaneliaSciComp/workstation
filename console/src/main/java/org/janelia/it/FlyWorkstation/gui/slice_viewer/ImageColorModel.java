package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.Color;
import java.util.Arrays;
import java.util.Vector;

import com.google.common.base.Joiner;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.raster.IntensityFormat;
import org.janelia.it.FlyWorkstation.signal.Signal;
import org.janelia.it.FlyWorkstation.signal.Slot1;

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
	
	private Vector<ChannelColorModel> channels = new Vector<ChannelColorModel>();
	private boolean blackSynchronized = true;
	private boolean gammaSynchronized = true;
	private boolean whiteSynchronized = true;

	private Signal colorModelChangedSignal = new Signal();
	private Signal colorModelInitializedSignal = new Signal();
	
	/*
	public ImageColorModel() {
		init(3, 8); // Default to 3 8-bit channels
	}
	**/
	
	public ImageColorModel(VolumeImage3d volumeImage) 
	{
		reset(volumeImage);
	}

    /**
     * return a string that represents the user-adjustable state of
     * the model for storage in preferences
     */
    public String asString() {
        // format: nchannels, then three locks, then one string for each channel:
        StringBuilder builder = new StringBuilder();
        builder.append(getChannelCount());
        builder.append(":");
        builder.append(isBlackSynchronized());
        builder.append(":");
        builder.append(isGammaSynchronized());
        builder.append(":");
        builder.append(isWhiteSynchronized());

        for (ChannelColorModel ch: channels) {
            builder.append(":");
            builder.append(ch.asString());
        }

        return builder.toString();
    }

    /**
     * parse and apply a string of the form returned by asString()
     */
    public void fromString(String modelString) {
        // this is going to be ugly; I supposed I could/should use
        //  a different delimiter for each channel info, but it seems
        //  inelegant and impure to multiply delimiters

        String [] items = modelString.split(":");
        // should be 4 + 8 * nchannels items at this point

        if (getChannelCount() != Integer.parseInt(items[0])) {
            // must be same number of channels!
            return;
        }

        // in principle I could pass the post-split array,
        //  but I like having both fromString() methods look the same;
        //  so I reconstitute the delimited string and pass it down
        for (int i=0; i<getChannelCount(); i++) {
            // thankfully we have Guava, or this would be ugly;
            //  I can't believe Java doesn't have a string joiner in
            //  the standard lib!  or array slices!
            Joiner joiner = Joiner.on(":");
            String s = joiner.join(Arrays.copyOfRange(items, 4 + 8 * i, 4 + 8 * i + 8));
            channels.get(i).fromString(s);
        }

        // do syncs:
        setBlackSynchronized(Boolean.parseBoolean(items[1]));
        setGammaSynchronized(Boolean.parseBoolean(items[2]));
        setWhiteSynchronized(Boolean.parseBoolean(items[3]));

    }

	private void addChannel(Color color, int bitDepth) {
		int c = channels.size();
		ChannelColorModel channel = new ChannelColorModel(
				c, color, bitDepth);
		channels.add(channel);
		// connect signals
		channel.getColorChangedSignal().connect(colorModelChangedSignal);
		channel.getDataMaxChangedSignal().connect(colorModelChangedSignal);
		channel.getVisibilityChangedSignal().connect(colorModelChangedSignal);

		channel.getBlackLevelChangedSignal().connect(new Slot1<Integer>() {
			@Override
			public void execute(Integer blackLevel) {
				if (blackSynchronized) {
					for (ChannelColorModel channel : channels)
						channel.setBlackLevel(blackLevel);
				}
				colorModelChangedSignal.emit();
			}
		});

		channel.getGammaChangedSignal().connect(new Slot1<Double>() {
			@Override
			public void execute(Double gamma) {
				if (gammaSynchronized) {
					for (ChannelColorModel channel : channels)
						channel.setGamma(gamma);
				}
				colorModelChangedSignal.emit();
			}
		});

		channel.getWhiteLevelChangedSignal().connect(new Slot1<Integer>() {
			@Override
			public void execute(Integer whiteLevel) {
				if (whiteSynchronized) {
					for (ChannelColorModel channel : channels)
						channel.setWhiteLevel(whiteLevel);
				}
				colorModelChangedSignal.emit();
			}
		});
	}

	public Signal getColorModelInitializedSignal() {
		return colorModelInitializedSignal;
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

	public Signal getColorModelChangedSignal() {
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
		// System.out.println("model channel count = "+channelCount);
		resetColors(); // in case 1 or 2 channels
		colorModelInitializedSignal.emit();
	}
	
	public void reset(VolumeImage3d volumeImage) 
	{
		if (volumeImage == null)
			return;
		int maxI = volumeImage.getMaximumIntensity();
		assert maxI <= 65535;
		int bitDepth = 8;
		if (maxI > 255)
			bitDepth = 16;
		int hardMax = (int)Math.pow(2.0, bitDepth) - 1;
		init(volumeImage.getNumberOfChannels(), bitDepth);
		if (hardMax != maxI) {
			for (ChannelColorModel ccm : channels) {
				ccm.setWhiteLevel(maxI);
				ccm.setDataMax(maxI);
			}
		}
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
			getChannel(0).setColor(Color.yellow);
			getChannel(1).setColor(Color.cyan);
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
