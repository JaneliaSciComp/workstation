/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package janelia.lvv.tileloader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Christopher Bruns
 */
public class LoadTimeMeasurement
implements Iterable<Float>
{
    private final List<Float> timings = new ArrayList<Float>();
    private boolean statsAreDirty = false;
    private float cachedMean = Float.NaN;
    private float cachedStdDev = 0f;
    private float cachedMedian = Float.NaN;
    private float cachedMax = 0;
    private float cachedMin = Float.NaN;
    private float cachedTotalTime = 0;
    private final LoadStrategem selector;

    public LoadTimeMeasurement(BrickSliceLoader loader, LoadStrategem selector) throws IOException 
    {
        this.selector = selector;
        long t0 = System.nanoTime();
        for (SubstackInfo s : selector) {
            loader.loadSliceRange(s.brickFolder, s.sliceIndices);
            long t1 = System.nanoTime();
            long dT = t1 - t0;
            float ms = dT/1e6f; // convert to milliseconds
            add(ms);
            t0 = t1;
        }
    }
    
    public final boolean add(float ms)
    {
        statsAreDirty = true;
        return timings.add(ms);
    }

    public float get(int index)
    {
        return timings.get(index);
    }
    
    public boolean isEmpty() {
        return timings.isEmpty();
    }
    
    public float maximum() {
        checkStats();
        return cachedMax;
    }
    
    public float mean()
    {
        checkStats();
        return cachedMean;
    }
    
    public float median()
    {
        checkStats();
        return cachedMedian;
    }
    
    public float minimum()
    {
        checkStats();
        return cachedMin;
    }
    
    public void report(OutputStream out) {
        PrintStream ps = new PrintStream(out);
        String selectorName = selector.getClass().getSimpleName();
        ps.println("Slices selected using a " + selectorName);
        // ps.println("From location: " + selector.);
        ps.println("Total of " + size() + " slices loaded,");
        ps.println("in a total of " + totalTime() + " milliseconds.");
        ps.println("Median slice load took " + median() + " milliseconds.");
        ps.println("Mean slice load took " + mean() + " milliseconds.");
        ps.println("with a standard deviation of " + standardDeviation() + " milliseconds.");
        ps.println("First slice load took " + get(0) + " milliseconds.");
        ps.println("Minimum slice load took " + minimum() + " milliseconds.");
        ps.println("Maximum slice load took " + maximum() + " milliseconds.");
    }
    
    public int size() {
        return timings.size();
    }

    public float standardDeviation() {
        checkStats();
        return cachedStdDev;
    }
    
    public float totalTime() {
        checkStats();
        return cachedTotalTime;
    }
    
    private void checkStats()
    {
        if (! statsAreDirty) return;
        float sum = 0;
        int count = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = 0;
        for (float f : timings) {
            sum += f;
            count += 1;
            max = Math.max(f, max);
            min = Math.min(f, min);
        }
        cachedTotalTime = sum;
        float mean = sum / count;
        if (count < 2) {
            cachedStdDev = 0;
        } 
        else {
            sum = 0;
            // second pass for numerically stabler stdDev
            for (float f : timings) {
                float df = f - mean;
                sum += df*df;
            }
            cachedStdDev = (float)Math.sqrt(sum/(count - 1)); // sample standard deviation
        }
        cachedMean = mean;
        cachedMax = max;
        cachedMin = min;
        
        // median
        if (count < 1)
            cachedMedian = Float.NaN;
        else if (count == 1)
            cachedMedian = timings.get(0);
        else {
            List<Float> sorted = new ArrayList<Float>(timings);
            Collections.sort(sorted);
            int median_ix = (sorted.size() - 1) / 2;
            cachedMedian = sorted.get(median_ix);
        }
        
        statsAreDirty = false;
    }

    @Override
    public Iterator<Float> iterator()
    {
        return timings.iterator();
    }
    
}
