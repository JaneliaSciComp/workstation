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

package org.janelia.gltools.scenegraph;

import java.util.List;

/**
 *
 * @author Christopher Bruns
 */
class RenderViewport
{
    private int widthPixels;
    private int heightPixels;
    private int originLeftPixels;
    private int originBottomPixels;
    private float aspectRatio;
    
    // How much of the parent drawable surface does this viewport consume?
    private float originLeftRelative;
    private float originBottomRelative;
    private float widthRelative;
    private float heightRelative;

    public RenderViewport() {
        // Default to zero size
        this.widthPixels = 0;
        this.heightPixels = 0;
        this.originBottomPixels = 0;
        this.originLeftPixels = 0;

        // Default to occupying full canvas
        this.aspectRatio = 1.0f;
        this.heightRelative = 1.0f;
        this.widthRelative = 1.0f;
        this.originBottomRelative = 0.0f;
        this.originLeftRelative = 0.0f;
    }
    
    public int getWidthPixels()
    {
        return widthPixels;
    }

    public void setWidthPixels(int widthPixels)
    {
        this.widthPixels = widthPixels;
    }

    public int getHeightPixels()
    {
        return heightPixels;
    }

    public void setHeightPixels(int heightPixels)
    {
        this.heightPixels = heightPixels;
    }

    public int getOriginLeftPixels()
    {
        return originLeftPixels;
    }

    public void setOriginLeftPixels(int originLeftPixels)
    {
        this.originLeftPixels = originLeftPixels;
    }

    public int getOriginBottomPixels()
    {
        return originBottomPixels;
    }

    public void setOriginBottomPixels(int originBottomPixels)
    {
        this.originBottomPixels = originBottomPixels;
    }

    public float getAspectRatio()
    {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio)
    {
        this.aspectRatio = aspectRatio;
    }

    public float getOriginLeftRelative()
    {
        return originLeftRelative;
    }

    public void setOriginLeftRelative(float originLeftRelative)
    {
        this.originLeftRelative = originLeftRelative;
    }

    public float getOriginBottomRelative()
    {
        return originBottomRelative;
    }

    public void setOriginBottomRelative(float originBottomRelative)
    {
        this.originBottomRelative = originBottomRelative;
    }

    public float getWidthRelative()
    {
        return widthRelative;
    }

    public void setWidthRelative(float widthRelative)
    {
        this.widthRelative = widthRelative;
    }

    public float getHeightRelative()
    {
        return heightRelative;
    }

    public void setHeightRelative(float heightRelative)
    {
        this.heightRelative = heightRelative;
    }

    List<CameraNode> getCameras()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
