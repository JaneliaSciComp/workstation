/**
 * This class may be extended to carry out all CPU work towards loading and talking with shader programs
 * in the GPU.
 *
 * Supports only vertex and fragment shaders. Both must be non-null.
 */
package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class AbstractShader
{
    int vertexShader = 0;
    int fragmentShader = 0;
    int shaderProgram = 0;

    private static final String LINE_ENDING = System.getProperty( "line.separator" );

    /**  All abstract methods.  Implement these for the specifics. */
    public abstract String getVertexShader();
    public abstract String getFragmentShader();
    public abstract void load(GL2 gl);
    public abstract void unload(GL2 gl);

    public void init(GL2 gl) throws ShaderCreationException {
        // Create shader program
        vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        loadOneShader(vertexShader, getVertexShader(), gl);

        // System.out.println("loaded vertex shader");
        fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        loadOneShader(fragmentShader, getFragmentShader(), gl);

        // System.out.println("loaded fragment shader");
        shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);
        gl.glLinkProgram(shaderProgram);
        gl.glValidateProgram(shaderProgram);
        IntBuffer intBuffer = IntBuffer.allocate(1);
        gl.glGetProgramiv(shaderProgram, GL2.GL_LINK_STATUS, intBuffer);
        if (intBuffer.get(0) != 1) {
            gl.glGetProgramiv(shaderProgram, GL2.GL_INFO_LOG_LENGTH, intBuffer);
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
    }

	private void loadOneShader(int shaderId, String resourceName, GL2 gl) throws ShaderCreationException {
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
				stringBuffer.append(line);
				stringBuffer.append("\n");
			}
			String progString = stringBuffer.toString();
			// System.out.println(progString);
			gl.glShaderSource(shaderId, 1, new String[]{progString}, (int[])null, 0);
			gl.glCompileShader(shaderId);
			
			// query compile status and possibly read log
			int[] status = new int[1];
			gl.glGetShaderiv(shaderId, GL2.GL_COMPILE_STATUS, status, 0);
			if (status[0] == GL2.GL_TRUE){
				// System.out.println(resourceName + ": successful");
				// everything compiled successfully, no log
			} 
			else {
				// compile failed, read the log and return it
				gl.glGetShaderiv(shaderId, GL2.GL_INFO_LOG_LENGTH, status, 0);
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

    /** Throws this exception to indicate that a shader creation (load/compile/whatever) failed. */
    public static class ShaderCreationException extends Exception {
        public ShaderCreationException( String message ) {
            super( message );
        }
        public ShaderCreationException( String message, Throwable ex ) {
            super( message, ex );
        }
    }

}
