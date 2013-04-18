package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

public class ChannelBrightnessStats {
	private int min = Integer.MAX_VALUE;
	private int max = Integer.MIN_VALUE;

	public void combine(ChannelBrightnessStats other) {
		min = Math.min(this.getMin(), other.getMin());
		max = Math.max(this.getMax(), other.getMax());
	}
	
	public int getMin() {
		return min;
	}
	public void setMin(int min) {
		this.min = min;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	
}
