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

package org.janelia.horta.nodes;

import org.janelia.horta.modelapi.NeuronSegment;

/**
 *
 * @author Christopher Bruns
 */
public class BasicNeuronSegment implements NeuronSegment
{
    private double[] location = {0, 0, 0};
    private double radius = 1.0; // micrometers
    private int label = 1;
    private int parentLabel = -1;
    private int typeIndex = 0;

    BasicNeuronSegment(double x, double y, double z)
    {
        location[0] = x;
        location[1] = y;
        location[2] = z;
    }

    @Override
    public double[] getLocation()
    {
        return location;
    }

    public void setLocation(double[] location)
    {
        this.location = location;
    }

    @Override
    public void setLocation(double x, double y, double z)
    {
        location[0] = x;
        location[1] = y;
        location[2] = z;
    }

    @Override
    public double getRadius()
    {
        return radius;
    }

    @Override
    public void setRadius(double radius)
    {
        this.radius = radius;
    }

    @Override
    public int getLabel()
    {
        return label;
    }

    @Override
    public void setLabel(int label)
    {
        this.label = label;
    }

    @Override
    public int getParentLabel()
    {
        return parentLabel;
    }

    @Override
    public void setParentLabel(int parentLabel)
    {
        this.parentLabel = parentLabel;
    }

    @Override
    public int getTypeIndex()
    {
        return typeIndex;
    }

    @Override
    public void setTypeIndex(int typeIndex)
    {
        this.typeIndex = typeIndex;
    }

    
}
