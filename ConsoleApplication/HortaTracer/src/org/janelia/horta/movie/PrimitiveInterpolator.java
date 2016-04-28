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

package org.janelia.horta.movie;

import org.janelia.geometry3d.Quaternion;

/**
 * Interpolates Java primitive types, plus Quaternions
 * @author brunsc
 */
public class PrimitiveInterpolator 
{
    private final InterpolatorKernel kernel;
    
    public PrimitiveInterpolator(InterpolatorKernel kernel) {
        this.kernel = kernel;
    }
    
    // Assumes points are spaced equally
    double interpolate_equidistant(double t, double p0, double p1, double p2, double p3)
    {
        return kernel.interpolate_equidistant(t, p0, p1, p2, p3);
    }
    
    // General case does not require points to be spaced equally
    double interpolate(double t, // t in range [0-1], between points p1 and p2
            double p0, double p1, double p2, double p3, // values at 4 points
            double t0, double t1, double t2, double t3) // distribution of 4 points along x axis
    {
        // Logger logger = LoggerFactory.getLogger(this.class);
        
        double result = p1;

        // Scale values to make them as-if equidistant
        double p0s = p0;
        double p3s = p3;
        if (t1 == t2) { // no need to interpolate if interval is empty
            result = (p1 + p2)/2.0;
        }
        else {
            if (t0 != t1) // avoid giving infinity weight to duplicated points
                p0s *= (t2-t1)/(t1-t0);
            if (t2 != t3)
                p3s *= (t2-t1)/(t3-t2);
            result = interpolate_equidistant(t, p0s, p1, p2, p3s);
        }
        // logger.info("Interpolating "+t+" ["+p0s+"("+p0+"),"+p1+","+p2+","+p3s+"("+p3+")] to "+result);
        return result;
    }
    
    // Specialization for floats
    float interpolate_equidistant(double t, float p0, float p1, float p2, float p3)
    {
        return (float) interpolate_equidistant(t, (double)p0, (double)p1, (double)p2, (double)p3);
    }
    float interpolate(double t, 
            float p0, float p1, float p2, float p3,
            double t0, double t1, double t2, double t3) 
    {
        return (float)interpolate(t, 
                (double)p0, (double)p1, (double)p2, (double)p3,
                t0, t1, t2, t3);
    }
    
    // Specialization for integers
    int interpolate_equidistant(double t, int p0, int p1, int p2, int p3)
    {
        return (int)Math.round(interpolate_equidistant(t, (double)p0, (double)p1, (double)p2, (double)p3));
    }
    int interpolate(double t, 
            int p0, int p1, int p2, int p3,
            double t0, double t1, double t2, double t3) 
    {
        return (int)Math.round(interpolate(t, 
                (double)p0, (double)p1, (double)p2, (double)p3,
                t0, t1, t2, t3));
    }
    
    // Specialization for boolean values
    boolean interpolate_equidistant(double t, boolean p0, boolean p1, boolean p2, boolean p3)
    {
        double d0 = p0 ? 1.0 : 0.0;
        double d1 = p0 ? 1.0 : 0.0;
        double d2 = p0 ? 1.0 : 0.0;
        double d3 = p0 ? 1.0 : 0.0;
        double result = interpolate_equidistant(t, d0, d1, d2, d3);
        return result >= 0.5;
    }
    boolean interpolate(double t, 
            boolean p0, boolean p1, boolean p2, boolean p3,
            double t0, double t1, double t2, double t3) 
    {
        double d0 = p0 ? 1.0 : 0.0;
        double d1 = p0 ? 1.0 : 0.0;
        double d2 = p0 ? 1.0 : 0.0;
        double d3 = p0 ? 1.0 : 0.0;
        double result = interpolate(t, d0, d1, d2, d3, t0, t1, t2, t3);
        return result >= 0.5;
    }
    
    // specialization for Quaterions
    // Translated from page 449 of "Visualizing Quaternions" by Andrew J. Hanson.
    // TODO: I have no idea how to do a non-uniform version of Quaternion interpolation.
    Quaternion interpolate_equidistant(double t, Quaternion q0, Quaternion q1, Quaternion q2, Quaternion q3)
    {
        return kernel.interpolate_equidistant(t, q0, q1, q2, q3);
    }
    
}
