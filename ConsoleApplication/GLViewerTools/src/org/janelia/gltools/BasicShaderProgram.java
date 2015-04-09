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
package org.janelia.gltools;

import com.jogamp.opengl.util.glsl.ShaderUtil;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;
import javax.media.opengl.GLException;

/**
 * Represents an OpenGL shader
 * 
 * @author brunsc
 */
public abstract class BasicShaderProgram implements ShaderProgram {
    protected int programHandle = 0;
    protected int previousProgramHandle = 0;
    private final IntBuffer pp = IntBuffer.allocate(1);
    private final List<ShaderStep> shaderSteps = new ArrayList<ShaderStep>();

    public BasicShaderProgram() {
    }

    // Derived classes should insert shaders in list returned by getShaderSteps()
    public List<ShaderStep> getShaderSteps() {
        return shaderSteps;
    }
    
    @Override
    public void init(GL3 gl) {
        if (programHandle != 0)
            return; // already initialized
        programHandle = gl.glCreateProgram();
        for (ShaderStep step : shaderSteps) {
            step.init(gl);
            gl.glAttachShader(programHandle, step.id());
        }
        gl.glLinkProgram(programHandle);
        if ( ! ShaderUtil.isProgramStatusValid(gl, programHandle, GL3.GL_LINK_STATUS) ) 
        {
            // TODO - handle link error
            String log = ShaderUtil.getProgramInfoLog(gl, programHandle);
            throw new GLException("Shader link failed: \n" + log);
        }
        
        // Clean up
        for (ShaderStep step : shaderSteps) {
            gl.glDetachShader(programHandle, step.id());
            step.dispose(gl);
        }
    }
    
    @Override
    public int load(GL3 gl) {
        if (programHandle == 0) {
            init(gl);
        }
        if (programHandle == 0) {
            return previousProgramHandle;
        }
        pp.rewind();
        gl.glGetIntegerv(GL3.GL_CURRENT_PROGRAM, pp);
        previousProgramHandle = pp.get(0);
        if (programHandle != 0) {
            gl.glUseProgram(programHandle);
        }
        return previousProgramHandle;
    }

    @Override
    public void unload(GL3 gl) {
        if (programHandle != 0) {
            gl.glUseProgram(previousProgramHandle);
        }
    }

    @Override
    public int getProgramHandle() {
        return programHandle;
    }

    @Override
    public void dispose(GL3 gl) {
        if (programHandle != 0) {
            gl.glDeleteProgram(programHandle);
        }
        programHandle = 0;
    }
    
}
