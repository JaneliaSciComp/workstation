package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.util.HashMap;
import java.util.Vector;

public class LoadTimer {
	private String previousStepName = null;
	private long previousTime = 0;
	private HashMap<String, Vector<Double>> data = new HashMap<String, Vector<Double>>();

	public synchronized void putAll(LoadTimer other) {
		if (other == this)
			return;
		for (String key : other.data.keySet()) {
			if (! data.containsKey(key))
				data.put(key, other.data.get(key));
			else {
				data.get(key).addAll(other.data.get(key));
			}
		}
	}
	
	// Append a named timing waypoint to the timing statistics
	public synchronized void mark(String stepName) {
		long t0 = previousTime;
		long t1 = System.nanoTime();
		if (previousStepName != null) {
			double deltaMs = (t1 - t0)/1e6;
			String key = previousStepName + " --> " + stepName;
			if (! data.containsKey(key))
				data.put(key, new Vector<Double>());
			Vector<Double> values = data.get(key);
			values.add(deltaMs);
		}
		previousStepName = stepName;
		previousTime = t1;
	}
	
	public synchronized void report() {
		for (String interval : data.keySet()) {
			Vector<Double> values = data.get(interval);
			if (values.size() < 1)
				continue; // no data for this interval
			System.out.println(interval + ": ");
			double max = values.get(0);
			double min = values.get(0);
			double sum = 0.0;
			for (double value : values) {
				sum += value;
				if (value > max)
					max = value;
				if (value < min)
					min = value;
			}
			double mean = sum / values.size();
			// second pass for standard deviation
			sum = 0.0;
			for (double value : values) {
				double dv = value - mean;
				dv = dv * dv;
				sum += dv;
			}
			double variance = 0.0;
			if (values.size() > 1) {
				variance = sum / (values.size() - 1);
			}
			double standardDeviation = Math.sqrt(variance);

			System.out.println("  mean = "+mean+" ms");
			System.out.println("  stdDev = "+standardDeviation+" ms");
			System.out.println("  n = "+values.size());
			System.out.println("  min = "+min+" ms");
			System.out.println("  max = "+max+" ms");
		}
	}
}
