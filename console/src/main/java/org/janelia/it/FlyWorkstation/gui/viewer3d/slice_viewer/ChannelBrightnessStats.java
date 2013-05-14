package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.util.Arrays;

public class ChannelBrightnessStats {
	private int min = Integer.MAX_VALUE;
	private int max = Integer.MIN_VALUE;
	private int histogram[] = new int[256];

	public void combine(ChannelBrightnessStats other) {
		int oldMin = min;
		double dBin = (max - min) / 256.0; // remember old binning
		min = Math.min(this.getMin(), other.getMin());
		max = Math.max(this.getMax(), other.getMax());
		// Update old histogram for new range
		int oldHistogram[] = histogram;
		histogram = new int[256];
		clearHistogram();
		// map old histogram
		for (int i = 0; i < 256; ++i) {
			int val = (int)Math.round(oldMin + dBin*(i+0.5));
			int count = oldHistogram[i];
			if (count > 0)
				updateHistogram(val, count);
		}
		// Combine with other histogram
		dBin = (other.max - other.min) / 256.0;
		for (int i = 0; i < 256; ++i) {
			int val = (int)Math.round(other.min + dBin*(i+0.5));
			int count = other.histogram[i];
			updateHistogram(val, count);
		}
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

	public void clearHistogram() {
		Arrays.fill(histogram, 0);
	}

	/**
	 * updateHistogram(int) only works AFTER max and min have been set correctly.
	 * @param val
	 */
	public void updateHistogram(int val, int count) {
		if (count < 1)
			return;
		int bin = (int)(255.99 * ((float)val - min) / (max - min));
		if (bin > 255)
			bin = 255;
		if (bin < 0)
			bin = 0;
		histogram[bin] += count;
	}
	
	/**
	 * Histogram must be populated for this to work
	 * @param quantile
	 * @return
	 */
	public int estimateQuantile(double quantile) {
		int hCount = 0;
		for (int i = 0; i < 256; ++i) {
			hCount += histogram[i];
		}
		if (hCount == 0)
			return 0;
		if (quantile <= 0.0)
			return min;
		if (quantile >= 1.0)
			return max;
		double targetCount = hCount * quantile;
		hCount = 0;
		int bin = 0;
		double binFraction = 0;
		for (int i = 0; i < 256; ++i) {
			bin = i;
			hCount += histogram[i];
			if (hCount >= targetCount) {
				binFraction = (hCount - targetCount) / histogram[i]; // estimate of how far into the bin to get this intensity
				break;
			}
		}
		// assume flat distribution within bin
		double dBin = (max - min)/256.0;
		double i0 = min + bin*dBin;
		double result = i0 + binFraction * dBin;
		return (int)Math.round(result);
	}
	
}
