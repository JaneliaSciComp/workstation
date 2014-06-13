package org.janelia.it.workstation.gui.slice_viewer;

import java.util.Vector;

public class ImageBrightnessStats 
extends Vector<ChannelBrightnessStats>
{
	private static final long serialVersionUID = 1L;
	
	public void combine(ImageBrightnessStats other) {
		// combine results channel by channel
		for (int c = 0; c < this.size(); ++c) {
			if (c >= other.size())
				break; // other stats has viewer channels
			get(c).combine(other.get(c));
		}
		// Does other have more channels than us? Fetch those channels unchanged
		for (int c = size(); c < other.size(); ++c) {
			add(other.get(c));
		}
	}
	
	public int getMin() {
		int result = Integer.MAX_VALUE;
		for (ChannelBrightnessStats bs : this)
			result = Math.min(result, bs.getMin());
		if (result == Integer.MAX_VALUE)
			result = 0; // default to zero
		return result;
	}

	public int getMax() {
		int result = Integer.MIN_VALUE;
		for (ChannelBrightnessStats bs : this)
			result = Math.max(result, bs.getMax());
		if (result == Integer.MIN_VALUE)
			result = 255; // default to 255
		return result;
	}
}
