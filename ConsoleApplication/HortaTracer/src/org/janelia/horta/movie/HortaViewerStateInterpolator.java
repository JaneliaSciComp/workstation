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
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.NeuronTracerTopComponent.HortaViewerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
class HortaViewerStateInterpolator implements Interpolator<HortaViewerState> 
{
    private final InterpolatorKernel defaultKernel = 
            // new LinearInterpolatorKernel();
            new CatmullRomSplineKernel();
    private final InterpolatorKernel linearKernel = 
            new LinearInterpolatorKernel();
    private final Interpolator<Vector3> vec3Interpolator = new Vector3Interpolator(defaultKernel);
    private final PrimitiveInterpolator primitiveInterpolator = new PrimitiveInterpolator(defaultKernel);
    private final Interpolator<Quaternion> rotationInterpolator = new PrimitiveInterpolator(defaultKernel);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HortaViewerStateInterpolator() {
    }

    @Override
    public HortaViewerState interpolate_equidistant(
            double ofTheWay, 
            HortaViewerState p0, HortaViewerState p1, HortaViewerState p2, HortaViewerState p3) 
    {

        Vector3 focus = vec3Interpolator.interpolate_equidistant(ofTheWay, 
                new Vector3(p0.getCameraFocus()), 
                new Vector3(p1.getCameraFocus()), 
                new Vector3(p2.getCameraFocus()), 
                new Vector3(p3.getCameraFocus()));
        
        Quaternion rotation = rotationInterpolator.interpolate_equidistant(
                ofTheWay,
                p0.getCameraRotation(), 
                p1.getCameraRotation(), 
                p2.getCameraRotation(), 
                p3.getCameraRotation()
        );
        
        float zoom = primitiveInterpolator.interpolate_equidistant(
                ofTheWay,
                p0.getCameraSceneUnitsPerViewportHeight(),
                p1.getCameraSceneUnitsPerViewportHeight(),
                p2.getCameraSceneUnitsPerViewportHeight(),
                p3.getCameraSceneUnitsPerViewportHeight());
                
        HortaViewerState result = new HortaViewerState(
                focus.getX(), focus.getY(), focus.getZ(),
                rotation,
                zoom
        );
        
        return result;
    }

    @Override
    public HortaViewerState interpolate(
            double ofTheWay, 
            HortaViewerState p0, HortaViewerState p1, HortaViewerState p2, HortaViewerState p3, 
            double t0, double t1, double t2, double t3) 
    {
        Vector3 focus = vec3Interpolator.interpolate(ofTheWay, 
                new Vector3(p0.getCameraFocus()), 
                new Vector3(p1.getCameraFocus()), 
                new Vector3(p2.getCameraFocus()), 
                new Vector3(p3.getCameraFocus()), 
                t0, t1, t2, t3);
        
        Quaternion rotation = rotationInterpolator.interpolate(
                ofTheWay,
                p0.getCameraRotation(), 
                p1.getCameraRotation(), 
                p2.getCameraRotation(), 
                p3.getCameraRotation(), 
                t0, t1, t2, t3
        );
                
        float zoom = primitiveInterpolator.interpolate(
                ofTheWay,
                p0.getCameraSceneUnitsPerViewportHeight(),
                p1.getCameraSceneUnitsPerViewportHeight(),
                p2.getCameraSceneUnitsPerViewportHeight(),
                p3.getCameraSceneUnitsPerViewportHeight(), 
                t0, t1, t2, t3);
                
        // logger.info("ofTheWay = "+ofTheWay);
        // logger.info("zoom = "+zoom);
        
        HortaViewerState result = new HortaViewerState(
                focus.getX(), focus.getY(), focus.getZ(),
                rotation,
                zoom
        );
        
        return result;
    }
    
}
