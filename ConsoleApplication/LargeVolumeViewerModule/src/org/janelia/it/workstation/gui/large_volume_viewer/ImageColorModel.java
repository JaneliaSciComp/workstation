package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.Color;
import java.util.Arrays;
import java.util.Vector;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collection;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ChannelColorChangeListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ColorModelInitListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ColorModelListener;
//import org.janelia.it.workstation.signal.Signal;
//import org.janelia.it.workstation.signal.Slot1;

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
	
	private Vector<ChannelColorModel> channels = new Vector<>();
	private boolean blackSynchronized = true;
	private boolean gammaSynchronized = true;
	private boolean whiteSynchronized = true;
    
    private Collection<ColorModelListener> colorModelListeners = new ArrayList<>();
    private Collection<ColorModelInitListener> colorModelInitListeners = new ArrayList<>();

//	private Signal colorModelChangedSignal = new Signal();
//	private Signal colorModelInitializedSignal = new Signal();
	
	/*
	public ImageColorModel() {
		init(3, 8); // Default to 3 8-bit channels
	}
	**/
	
	public ImageColorModel(Integer maxIntensity, Integer numberOfChannels)
	{
		reset(maxIntensity, numberOfChannels);
	}

    /**
     * @param colorModelListener the colorModelListener to set
     */
    public void addColorModelListener(ColorModelListener colorModelListener) {
        colorModelListeners.add(colorModelListener);
    }

    public void removeColorModelListener(ColorModelListener l) {
        colorModelListeners.remove(l);
    }
    
    public void addColorModelInitListener(ColorModelInitListener listener) {
        colorModelInitListeners.add(listener);
    }
    
    public void removeColorModelInitListener(ColorModelInitListener listener) {
        colorModelInitListeners.remove(listener);
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

        // debug:
        // System.out.println("color model string in: " + modelString);
        int itemNo = 0;
        if (getChannelCount() != Integer.parseInt(items[itemNo++])) {
            // must be same number of channels!
            return;
        }

        // do syncs first, so later sets don't get unset if they
        //  shouldn't be
        setBlackSynchronized(Boolean.parseBoolean(items[itemNo++]));
        setGammaSynchronized(Boolean.parseBoolean(items[itemNo++]));
        setWhiteSynchronized(Boolean.parseBoolean(items[itemNo++]));

        // in principle I could pass the post-split array,
        //  but I like having both fromString() methods look the same;
        //  so I reconstitute the delimited string and pass it down
        int itemCount = (items.length - 4) / getChannelCount();
        for (int i=0; i<getChannelCount(); i++) {
            // thankfully we have Guava, or this would be ugly;
            //  I can't believe Java doesn't have a string joiner in
            //  the standard lib!  or array slices!
            Joiner joiner = Joiner.on(":");             
            String s = joiner.join(Arrays.copyOfRange(items, 4 + itemCount * i, 4 + itemCount * i + itemCount));
            channels.get(i).fromString(s);
        }

        // debug:
        // System.out.println("color model string as set: " + asString());

    }
    
    public void fireColorModelChanged() {
        for (ColorModelListener colorModelListener: colorModelListeners) {
            colorModelListener.colorModelChanged();
        }
    }

    public void fireColorModelInitialized() {
        for (ColorModelInitListener l: colorModelInitListeners) {
            l.colorModelInit();
        }
    }

    private void blackLevelChanged(Integer blackLevel) {
        if (blackSynchronized) {
            for (ChannelColorModel channel : channels) {
                channel.setBlackLevel(blackLevel);
            }
        }
        fireColorModelChanged();
    }

    private void gammaChanged(Double gamma) {
        if (gammaSynchronized) {
            for (ChannelColorModel channel : channels) {
                channel.setGamma(gamma);
            }
        }
        fireColorModelChanged();
    }
    
    private void whiteLevelChanged(Integer whiteLevel) {
        if (whiteSynchronized) {
            for (ChannelColorModel channel : channels) {
                channel.setWhiteLevel(whiteLevel);
            }
        }
        fireColorModelChanged();
    }

    private void addChannel(Color color, int bitDepth) {
		int c = channels.size();
		ChannelColorModel channel = new ChannelColorModel(
				c, color, bitDepth);
        // Add color model listener to just propagate the change signal.
        channel.setColorModelListener(new ColorModelListener() {
            @Override
            public void colorModelChanged() {
                fireColorModelChanged();
            }            
        });
		channels.add(channel);
		// connect signals
//		channel.getColorChangedSignal().connect(colorModelChangedSignal);
//		channel.getDataMaxChangedSignal().connect(colorModelChangedSignal);
//		channel.getVisibilityChangedSignal().connect(colorModelChangedSignal);

        channel.setChannelColorChangeListener(new ChannelColorChangeListener() {
            @Override
            public void blackLevelChanged(Integer blackLevel) {
                ImageColorModel.this.blackLevelChanged(blackLevel);
            }

            @Override
            public void gammaChanged(Double gamma) {
                ImageColorModel.this.gammaChanged(gamma);
            }

            @Override
            public void whiteLevelChanged(Integer whiteLevel) {
                ImageColorModel.this.whiteLevelChanged(whiteLevel);
            }            
        });
        
//		channel.getBlackLevelChangedSignal().connect(new Slot1<Integer>() {
//			@Override
//			public void execute(Integer blackLevel) {
//                ImageColorModel.this.blackLevelChanged(blackLevel);
////				colorModelChangedSignal.emit();
//			}
//		});

//		channel.getGammaChangedSignal().connect(new Slot1<Double>() {
//			@Override
//			public void execute(Double gamma) {
//                gammaChanged(gamma);
////				colorModelChangedSignal.emit();
//			}
//		});

//		channel.getWhiteLevelChangedSignal().connect(new Slot1<Integer>() {
//			@Override
//			public void execute(Integer whiteLevel) {
//                whiteLevelChanged(whiteLevel);
////				colorModelChangedSignal.emit();
//			}
//		});
	}

//	public Signal getColorModelInitializedSignal() {
//		return colorModelInitializedSignal;
//	}
//
	public boolean isBlackSynchronized() {
		return blackSynchronized;
	}

	public void setBlackSynchronized(boolean blackSynchronized) {
        if (blackSynchronized != this.blackSynchronized) {
		    this.blackSynchronized = blackSynchronized;
            fireColorModelChanged();
//				colorModelChangedSignal.emit();
        }
	}

	public boolean isGammaSynchronized() {
		return gammaSynchronized;
	}

	public void setGammaSynchronized(boolean gammaSynchronized) {
        if (gammaSynchronized != this.gammaSynchronized) {
		    this.gammaSynchronized = gammaSynchronized;
            fireColorModelChanged();
//				colorModelChangedSignal.emit();
        }
	}

	public boolean isWhiteSynchronized() {
		return whiteSynchronized;
	}

	public void setWhiteSynchronized(boolean whiteSynchronized) {
        if (whiteSynchronized != this.whiteSynchronized) {
		    this.whiteSynchronized = whiteSynchronized;
            fireColorModelChanged();
//				colorModelChangedSignal.emit();
        }
	}

//	public Signal getColorModelChangedSignal() {
//		return colorModelChangedSignal;
//	}

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
        fireColorModelInitialized();
//		colorModelInitializedSignal.emit();
	}
	
	public void reset(Integer maxI, Integer numberOfChannels)
	{
		if (maxI == null)
			return;
		assert maxI <= 65535;
		int bitDepth = 8;
		if (maxI > 255)
			bitDepth = 16;
		int hardMax = (int)Math.pow(2.0, bitDepth) - 1;
		init(numberOfChannels, bitDepth);
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
			getChannel(0).setColor(Color.green);
			getChannel(1).setColor(Color.magenta);
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
