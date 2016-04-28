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
 * Spline function for smooth interpolation of viewer states.
 * Catmull-Rom spline has the special property that it passes exactly
 * through all control points, while maintaining continuous first
 * derivatives, resulting in a smooth and graceful fly-through that
 * exactly visits each of the input key frames.
 * 
 * Notice that we include a spherical spline interpolation of Quaternions,
 * which can be used to get even the camera rotation to look very smooth, eliminating
 * that pop-and-lock 1970s robot-dance look that lesser animation tools
 * often exhibit.
 * 
 * @author brunsc
 */
public class CatmullRomSplineKernel implements InterpolatorKernel
{
    
    // Assumes points are spaced equally
    @Override
    public double interpolate_equidistant(double t, double p0, double p1, double p2, double p3)
    {
        return (
              t*((2.0-t)*t - 1.0) * p0
            + (t*t*(3.0*t - 5.0) + 2.0) * p1
            + t*((4.0 - 3.0*t)*t + 1.0) * p2
            + (t-1.0)*t*t * p3 ) / 2.0;
    }

    // specialization for Quaterions
    // Translated from page 449 of "Visualizing Quaternions" by Andrew J. Hanson.
    // TODO: I have no idea how to do a non-uniform version of Quaternion interpolation.
    @Override
    public Quaternion interpolate_equidistant(double t, Quaternion q00, Quaternion q01, Quaternion q02, Quaternion q03)
    {
        Quaternion q10 = q00.slerp(q01, (float)t+1);
        Quaternion q11 = q01.slerp(q03, (float)t);
        Quaternion q12 = q02.slerp(q03, (float)t-1);
        
        Quaternion q20 = q10.slerp(q11, (float)(t+1)/2f);
        Quaternion q21 = q11.slerp(q12, (float)t/2f);
        
        Quaternion result = q20.slerp(q21, (float)t);
        return result;
    }

}
