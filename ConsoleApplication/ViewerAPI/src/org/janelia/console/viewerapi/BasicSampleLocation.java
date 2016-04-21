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

package org.janelia.console.viewerapi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.janelia.console.viewerapi.model.NeuronModel;

/**
 *
 * @author Christopher Bruns
 */
public class BasicSampleLocation implements SampleLocation
{
    private URL sampleUrl = null;
    private double focusXUm = 0;
    private double focusYUm = 0;
    private double focusZUm = 0;
    private double micrometersPerWindowHeight = 100;
    private int defaultColorChannel = 0;
    private boolean compressed = false;
    private Long workspaceId = null;  // Optional
    private Long sampleId = null;     // Optional

    public BasicSampleLocation()
    {
    }

    @Override
    public URL getSampleUrl()
    {
        return sampleUrl;
    }

    @Override
    public void setSampleUrl(URL sampleUrl)
    {
        this.sampleUrl = sampleUrl;
    }

    @Override
    public double getFocusXUm()
    {
        return focusXUm;
    }

    public void setFocusXUm(double focusXUm)
    {
        this.focusXUm = focusXUm;
    }

    @Override
    public double getFocusYUm()
    {
        return focusYUm;
    }

    public void setFocusYUm(double focusYUm)
    {
        this.focusYUm = focusYUm;
    }

    @Override
    public double getFocusZUm()
    {
        return focusZUm;
    }

    public void setFocusZUm(double focusZUm)
    {
        this.focusZUm = focusZUm;
    }

    @Override
    public double getMicrometersPerWindowHeight()
    {
        return micrometersPerWindowHeight;
    }

    @Override
    public void setMicrometersPerWindowHeight(double micrometersPerWindowHeight)
    {
        this.micrometersPerWindowHeight = micrometersPerWindowHeight;
    }

    @Override
    public void setFocusUm(double x, double y, double z)
    {
        setFocusXUm(x);
        setFocusYUm(y);
        setFocusZUm(z);
    }

    @Override
    public int getDefaultColorChannel()
    {
        return defaultColorChannel;
    }

    @Override
    public void setDefaultColorChannel(int channelIndex)
    {
        defaultColorChannel = channelIndex;
    }

    @Override
    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    /**
     * Identifier for some workspace ID that may be known, about this sample.
     * Optional, since it cannot currently be provided by all callers.
     * 
     * @return 
     */
    @Override
    public Long getWorkspaceId() {
        return workspaceId;
    }
    
    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    /**
     * Identifier for some sample ID that may be known, about this sample.
     * Optional, since it cannot currently be provided by all callers.
     * 
     * @return 
     */
    @Override
    public Long getSampleId() {
        return sampleId;
    }
    
    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }
}
