
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
    private final List<ShaderStep> shaderSteps = new ArrayList<>();

    public BasicShaderProgram() {
    }

    // Derived classes should insert shaders in list returned by getShaderSteps()
    public final List<ShaderStep> getShaderSteps() {
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
