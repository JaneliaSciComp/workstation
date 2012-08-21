package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2;

public class VoxelRayShader 
{
    int vertexShader = 0;
    int fragmentShader = 0;
    int shaderProgram = 0;
    boolean shaderIsGood = false;
    int previousShader = 0;
    private int[] textureVoxels;
    private double[] voxelMicrometers;
	
    public void init(GL2 gl) {
		// Create shader program
		vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		if (loadOneShader(vertexShader, "shaders/VoxelRayVtx.glsl", gl)) {
			// System.out.println("loaded vertex shader");
			fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
			if (loadOneShader(fragmentShader, "shaders/VoxelRayFrg.glsl", gl)) {
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
					System.err.println("Program link error: ");
					if (size > 0) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(size);
						gl.glGetProgramInfoLog(shaderProgram, size, intBuffer, byteBuffer);
						for (byte b : byteBuffer.array()) {
							System.err.print((char)b);
						}
					} else {
						System.out.println("Unknown");
					}
				}
				else
					shaderIsGood = true;
			}
		}
    }
    
	public void load(GL2 gl) {
        if (shaderIsGood) {
            IntBuffer buffer = IntBuffer.allocate(1);
	        	gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, buffer);
	            previousShader = buffer.get();
	    		gl.glUseProgram(shaderProgram);
	    		// The default texture unit is 0.  Pass through shader works without setting volumeTexture.
	    		gl.glUniform1i(gl.glGetUniformLocation(shaderProgram, "volumeTexture"), 0);
	    		gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "textureVoxels"), 
	    				(float)textureVoxels[0], 
	    				(float)textureVoxels[1],
	    				(float)textureVoxels[2]);
	    		gl.glUniform3f(gl.glGetUniformLocation(shaderProgram, "voxelMicrometers"), 
	    				(float)voxelMicrometers[0], 
	    				(float)voxelMicrometers[1],
	    				(float)voxelMicrometers[2]);    		
	    }
	}
	
	public void unload(GL2 gl) {
		if (shaderIsGood) {
			gl.glUseProgram(previousShader);
		}
	}

	private boolean loadOneShader(int shaderId, String resourceName, GL2 gl) {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(
							getClass().getResourceAsStream(
									resourceName) ));
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
					System.out.println(resourceName + ": " + new String(log, 0, status[0]));
				} else
					System.out.println(resourceName + ": "+ "unknown compilation error");
				return false;
			}
			return true;
		} catch (Exception exc) {
			exc.printStackTrace();
		}		
		return false;
	}
	
	public void setUniforms(int[] textureVoxels, double[] voxelMicrometers) {
		this.textureVoxels = textureVoxels;
		this.voxelMicrometers = voxelMicrometers;
	}
	
}
