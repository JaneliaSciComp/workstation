
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
