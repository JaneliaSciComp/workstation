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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brunsc
 */
public class LinearPoint3DInterpolator 
implements Point3DInterpolator
{
    private final Interpolator<Number> lfi = new LinearNumberInterpolator();

    @Override
    public List<Number> interpolate(List<List<Number>> values, float ofTheWay, boolean isCircular) 
    {
        // Figure out bounds of where to look in the list of values
        int listIndex = (int) Math.floor(ofTheWay);
        float fraction = ofTheWay - listIndex;
        assert(fraction >= 0);
        assert(fraction <= 1);
        // Left edge value
        List<Number> v1 = values.get(listIndex);
        if (fraction <= 0) 
            return v1; // early termination at left edge
        // Right edge value
        List<Number> v2 = values.get(listIndex);
        assert(v1.size() == v2.size());
        // Interpolated value
        List<Number> result = new ArrayList<>();
        // Interpolate each element of the vector, one at a time
        for (int i = 0; i < v1.size(); ++i) {
            // We only need two items for linear interpolation, so delegate a sub-list to the Number interpolator
            List<Number> toInterpolate = new ArrayList<>();
            toInterpolate.add(v1.get(i));
            toInterpolate.add(v2.get(i));
            result.add(lfi.interpolate(toInterpolate, fraction, isCircular));
        }
        assert(result.size() == v1.size());
        return result;
    }
    
}
