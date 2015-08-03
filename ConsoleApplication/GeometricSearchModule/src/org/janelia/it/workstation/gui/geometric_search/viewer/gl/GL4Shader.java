/**
 * This class may be extended to carry out all CPU work towards loading and talking with shader programs
 * in the GPU.
 *
 * Supports only vertex and fragment shader. Both must be non-null.
 *
 *  BASED ON BasicShader by Christopher Bruns
 */

package org.janelia.it.workstation.gui.geometric_search.viewer.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

public abstract class GL4Shader
{

    protected static GLU glu = new GLU();

    private int vertexShader = 0;
    private int geometryShader = 0;
    private int fragmentShader = 0;
    private int shaderProgram = 0;
    private int previousShader = 0;

    private static final String LINE_ENDING = System.getProperty( "line.separator" );

    protected GLDisplayUpdateCallback updateCallback;

    /**  All abstract methods.  Implement these for the specifics. */
    public abstract String getVertexShaderResourceName();
    public String getGeometryShaderResourceName() { return null; }
    public abstract String getFragmentShaderResourceName();

    private Logger logger = LoggerFactory.getLogger( GL4Shader.class );

    private GL4ShaderProperties properties;

    public GL4Shader(GL4ShaderProperties properties) {
        this.properties=properties;
    }

    public GL4Shader() {}

    public void setProperties(GL4ShaderProperties properties) {
        this.properties=properties;
    }

    public void dispose(GL4 gl) {
        gl.glDeleteProgram(shaderProgram);
        shaderProgram = 0;
    }

    public void setUpdateCallback(GLDisplayUpdateCallback updateCallback) {
        this.updateCallback=updateCallback;
    }

    public void init(GL4 gl) throws ShaderCreationException {
        // Create shader program
        if ( getVertexShaderResourceName() != null ) {
            vertexShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
            logger.info("Loading vertex shader");
            loadOneShader(vertexShader, getVertexShaderResourceName(), gl);
            
            if (getGeometryShaderResourceName()!=null) {
                geometryShader = gl.glCreateShader(GL4.GL_GEOMETRY_SHADER);
                logger.info("Loading geometry shader");
                loadOneShader(geometryShader, getGeometryShaderResourceName(), gl);
            }

            // System.out.println("loaded vertex shader");
            fragmentShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);
            logger.info("Loading fragment shader");
            loadOneShader(fragmentShader, getFragmentShaderResourceName(), gl);

            // System.out.println("loaded fragment shader");
            if (shaderProgram == 0)
                shaderProgram = gl.glCreateProgram();
            logger.info("Attaching vertex shader");
            gl.glAttachShader(shaderProgram, vertexShader);
            if (geometryShader!=0) {
                logger.info("Attaching geometry shader");
                gl.glAttachShader(shaderProgram, geometryShader);
            }
            logger.info("Attaching fragment shader");
            gl.glAttachShader(shaderProgram, fragmentShader);
            logger.info("Linking program");
            gl.glLinkProgram(shaderProgram);
            logger.info("Validating program");
            gl.glValidateProgram(shaderProgram);
            logger.info("Post-validation");
            IntBuffer intBuffer = IntBuffer.allocate(1);
            logger.info("Retrieving link status");
            gl.glGetProgramiv(shaderProgram, GL4.GL_LINK_STATUS, intBuffer);
            logger.info("Checking link status value");
            if (intBuffer.get(0) != 1) {
                logger.info("Found problem - retrieving error log");
                gl.glGetProgramiv(shaderProgram, GL4.GL_INFO_LOG_LENGTH, intBuffer);
                int size = intBuffer.get(0);
                StringBuilder errBuilder = new StringBuilder();
                errBuilder.append("Problem with fragment shader ")
                        .append(fragmentShader)
                        .append(", program link error: ")
                        .append(LINE_ENDING);
                if (size > 0) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                    gl.glGetProgramInfoLog(shaderProgram, size, intBuffer, byteBuffer);
                    for (byte b : byteBuffer.array()) {
                        errBuilder.append((char) b);
                    }
                } else {
                    errBuilder.append("Unknown")
                            .append(LINE_ENDING);
                }
                throw new ShaderCreationException( errBuilder.toString() );

            }
            LongBuffer lb = LongBuffer.allocate(1);
            gl.glGetInteger64v(GL4.GL_MAX_SHADER_STORAGE_BLOCK_SIZE, lb);
            logger.info("MAX_SHADER_STORAGE_BLOCK_SIZE="+lb.get(0));
            gl.glGetInteger64v(GL4.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS, lb);
            logger.info("MAX_SHADER_STORAGE_BUFFER_BINDINGS="+lb.get(0));
        }
    }

    public void display(GL4 gl) {
        if (updateCallback!=null) {
            updateCallback.update(gl);
        }
    }

    public void load(GL4 gl) {
        int buf[] = {0};
        gl.glGetIntegerv( GL4.GL_CURRENT_PROGRAM, buf, 0 );
        previousShader = buf[0];
        gl.glUseProgram(shaderProgram);
    }

    private void loadOneShader(int shaderId, String resourceName, GL4 gl) throws ShaderCreationException {
        try {
            InputStream resourceStream = getClass().getResourceAsStream(
                    resourceName
            );
            if ( resourceStream == null ) {
                throw new Exception( "Unable to open resource " + resourceName );
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( resourceStream )
            );
            StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                if (properties!=null) {
                    line=scanForProperties(line);
                } 
                stringBuffer.append(line);
                stringBuffer.append("\n");
            }
            String progString = stringBuffer.toString();
            logger.debug("Shader contents: {}.", progString);
            gl.glShaderSource(shaderId, 1, new String[]{progString}, (int[])null, 0);
            gl.glCompileShader(shaderId);

            // query compile status and possibly read log
            int[] status = new int[1];
            gl.glGetShaderiv(shaderId, GL4.GL_COMPILE_STATUS, status, 0);
            if (status[0] == GL.GL_TRUE){
                // System.out.println(resourceName + ": successful");
                // everything compiled successfully, no log
            }
            else {
                // compile failed, read the log and return it
                gl.glGetShaderiv(shaderId, GL4.GL_INFO_LOG_LENGTH, status, 0);
                int maxLogLength = status[0];
                if (maxLogLength > 0) {
                    byte[] log = new byte[maxLogLength];
                    gl.glGetShaderInfoLog(shaderId, maxLogLength, status, 0, log, 0);
                    throw new ShaderCreationException( resourceName + ": " + new String(log, 0, status[0]) );
                } else
                    throw new ShaderCreationException( resourceName + ": "+ "unknown compilation error" );
            }

        } catch (Exception exc) {
            throw new ShaderCreationException( "Failed to load shader " + resourceName, exc );

        }
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    public boolean setUniform(GL4 gl, String varName, float value) {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniform1f( uniformLoc, value );
        return true;
    }

    public boolean setUniform(GL4 gl, String varName, int value)
    {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniform1i( uniformLoc, value );
        return true;
    }

    public boolean setUniform2fv(GL4 gl, String varName, int vecCount, float[] data)
    {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniform2fv( uniformLoc, vecCount, data, 0);
        return true;
    }

    public boolean setUniform3v(GL4 gl, String varName, int vecCount, float[] data)
    {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniform3fv( uniformLoc, vecCount, data, 0);
        return true;
    }

    public boolean setUniform4v(GL4 gl, String varName, int vecCount, float[] data)
    {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniform4fv( uniformLoc, vecCount, data, 0);
        return true;
    }

    public boolean setUniformMatrix2fv(GL4 gl, String varName, boolean transpose, float[] data) {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniformMatrix2fv( uniformLoc, 1, transpose, data, 0);
        return true;
    }

    public boolean setUniformMatrix4fv(GL4 gl, String varName, boolean transpose, float[] data) {
        int uniformLoc = gl.glGetUniformLocation( shaderProgram, varName );
        if ( uniformLoc < 0 )
            return false;
        gl.glUniformMatrix4fv( uniformLoc, 1, transpose, data, 0);
        return true;
    }

    public void unload(GL4 gl) {
        gl.glUseProgram(previousShader);
    }

    /** Throws this exception to indicate that a shader creation (load/compile/whatever) failed. */
    public static class ShaderCreationException extends Exception {
        public ShaderCreationException( String message ) {
            super( message );
        }
        public ShaderCreationException( String message, Throwable ex ) {
            super( message, ex );
        }
    }

    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

    public String scanForProperties(String line) {
        String[] tokens=line.split("\\s+");
        StringBuffer newLine=new StringBuffer();
        for (String token : tokens) {
            String value=properties.getStringValue(token);
            if (value!=null) {
                if (newLine.length()==0) {
                    newLine.append(value);
                } else {
                    newLine.append(" "+value);
                }
            } else {
                if (newLine.length()==0) {
                    newLine.append(token);
                } else {
                    newLine.append(" "+token);
                }
            }
        }
        return newLine.toString();
    }

}
