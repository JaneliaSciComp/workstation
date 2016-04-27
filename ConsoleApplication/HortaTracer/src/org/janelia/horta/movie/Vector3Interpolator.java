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

import org.janelia.geometry3d.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
class Vector3Interpolator implements Interpolator<Vector3> 
{
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public Vector3Interpolator() {
    }

    @Override
    public Vector3 interpolate_equidistant(double ofTheWay, Vector3 p0, Vector3 p1, Vector3 p2, Vector3 p3) {
        float x = CatmullRomSpline.interpolate_equidistant(ofTheWay, 
                p0.getX(), p1.getX(), p2.getX(), p3.getX());
        float y = CatmullRomSpline.interpolate_equidistant(ofTheWay, 
                p0.getY(), p1.getY(), p2.getY(), p3.getY());
        float z = CatmullRomSpline.interpolate_equidistant(ofTheWay, 
                p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ());
        
        return new Vector3(x, y, z);
    }

    @Override
    public Vector3 interpolate(
            double ofTheWay, 
            Vector3 p0, Vector3 p1, Vector3 p2, Vector3 p3, 
            double t0, double t1, double t2, double t3) 
    {
        float x = CatmullRomSpline.interpolate(ofTheWay, 
                p0.getX(), p1.getX(), p2.getX(), p3.getX(), 
                t0, t1, t2, t3);
        float y = CatmullRomSpline.interpolate(ofTheWay, 
                p0.getY(), p1.getY(), p2.getY(), p3.getY(), 
                t0, t1, t2, t3);
        float z = CatmullRomSpline.interpolate(ofTheWay, 
                p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), 
                t0, t1, t2, t3);
        
        return new Vector3(x, y, z);
    }
    
}
