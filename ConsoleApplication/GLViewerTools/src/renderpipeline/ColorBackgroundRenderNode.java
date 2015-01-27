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

package renderpipeline;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;
import org.janelia.gltools.ColorBackgroundActor;

/**
 * Paints a full screen solid color or color gradient to a RenderInput node
 * @author Christopher Bruns
 */
public class ColorBackgroundRenderNode implements RenderNode {
    
    private final ColorBackgroundActor actor;
    private final List<OutputPort> outputPorts = new ArrayList<OutputPort>();
    private final RenderTargetOutputPort rgbaOutput =
            new RenderTargetOutputPort(PixelFormat.class);
    
    public ColorBackgroundRenderNode() {
        actor = new ColorBackgroundActor(Color.PINK);
        outputPorts.add(rgbaOutput);
    }
    
    public void display(GL3 gl) {
        actor.display(gl, null, null);
    }    
    
    public void setColor(Color color) {
        actor.setColor(color);
    }

    @Override
    public List<InputPort> getInputPorts() {
        return null; // no input ports
    }

    @Override
    public List<OutputPort> getOutputPorts() {
        return outputPorts;
    }
}
