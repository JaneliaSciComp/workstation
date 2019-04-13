package org.janelia.geometry.util;

/**
 *
 * @author Christopher Bruns
 */
public class PerformanceTimer {
    private long previousTime;
    
    public PerformanceTimer() {
        this.previousTime = System.nanoTime();
    }
    
    public float reportMsAndRestart() {
        long newTime = System.nanoTime();
        float result = (newTime - previousTime) / 1e6f;
        previousTime = newTime;
        return result;
    }
}
