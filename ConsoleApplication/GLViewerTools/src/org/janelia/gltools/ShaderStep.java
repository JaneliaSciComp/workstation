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
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLException;
import org.apache.commons.io.IOUtils;

/**
 * Compiles a vertex or fragment shader
 * 
 * @author Christopher Bruns
 */
public class ShaderStep implements GL3Resource {
    private int shaderHandle;
    private final int shaderType;
    private final String programText;

    /**
     * Raises a GLException if the compile fails.
     * @param shaderType GL_VERTEX_SHADER or GL_FRAGMENT_SHADER or whatever
     * @param programText The shader program itself, as a string.
     */
    public ShaderStep(int shaderType, String programText) {
        this.shaderType = shaderType;
        this.programText = programText;
    }
    
    public ShaderStep(int shaderType, InputStream programStream) throws IOException {
        this.shaderType = shaderType;
        String programTextFromStream = IOUtils.toString(programStream);
        this.programText = programTextFromStream;
    }
    
    public int id() {
        return shaderHandle;
    }

    @Override
    public void dispose(GL3 gl) {
        gl.glDeleteShader(shaderHandle);
    }

    @Override
    public void init(GL3 gl) {
        shaderHandle = gl.glCreateShader(shaderType);
        gl.glShaderSource(shaderHandle, 1, new String[] {programText}, null);
        gl.glCompileShader(shaderHandle);
        IntBuffer result = IntBuffer.allocate(1);
        gl.glGetShaderiv(shaderHandle, GL2ES2.GL_COMPILE_STATUS, result);
        if (result.get(0) == GL2ES2.GL_FALSE) {
            // Compile failed
            String log = ShaderUtil.getShaderInfoLog(gl, shaderHandle);
            throw new GLException("Shader compile failed: \n" + log);
        }        
    }
    
}
