package org.janelia.it.workstation.gui.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

// Intended to eventually back actors like MeshActor and VolumeActor
public class VertexBuffer {
    // semantic values for computing byte array sizes
    private static final int bytesPerFloat = Float.SIZE/8; // 4;
    private static final int floatsPerNormal = 3;
    private static final int floatsPerColor = 3;
    private static final int floatsPerTextureCoordinate = 3;
    private static final int floatsPerVertex = 3;

    private static final boolean hasVertices = true; // This one is not negotiable
    
	private boolean hasNormals = false;
	private boolean hasColors = false;
	private boolean hasTextureCoordinates = false;

	private int vbo = 0;
	private boolean initialized = false;

	// Offsets for vertex attributes within packed array
	private int vertexByteOffset = 0;
	private int normalByteOffset = 0;
	private int colorByteOffset = 0;
	private int textureCoordinateByteOffset = 0;
	
	private float vertexArray[];
	private float normalArray[];
	private float colorArray[];
	private float textureCoordinateArray[];
	
	public void init(GL2GL3 gl2gl3) {
		GL gl = gl2gl3.getGL();

		int[] ix = {0};
		gl.glGenBuffers(1, ix, 0);
		vbo = ix[0];

		// Compute backed byte offset
		int vertexByteCount = 0;
		int byteOffset = 0;
		if (hasVertices) {
			int db = bytesPerFloat * floatsPerVertex;
			vertexByteCount += db;
			vertexByteOffset = byteOffset;
			byteOffset += db;
		}
		if (hasNormals) {
			normalByteOffset = byteOffset;
			int db = bytesPerFloat * floatsPerNormal;
			vertexByteCount += db;
			normalByteOffset = byteOffset;
			byteOffset += db;
		}
		if (hasColors) {
			colorByteOffset = byteOffset;
			int db = bytesPerFloat * floatsPerColor;
			vertexByteCount += db;
			colorByteOffset = byteOffset;
			byteOffset += db;
		}
		if (hasTextureCoordinates) {
			textureCoordinateByteOffset = byteOffset;
			int db = bytesPerFloat * floatsPerTextureCoordinate;
			vertexByteCount += db;
			textureCoordinateByteOffset = byteOffset;
			byteOffset += db;
		}
		int totalBufferByteCount = vertexByteCount * vertexArray.length/3;
		ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(totalBufferByteCount);
		vertexByteBuffer.order(ByteOrder.nativeOrder());
		FloatBuffer vertices = vertexByteBuffer.asFloatBuffer();
		vertices.rewind();
		for (int i = 0; i < vertexArray.length; i += 3) {
			if (hasVertices) {
				vertices.put(vertexArray[i]);
				vertices.put(vertexArray[i+1]);
				vertices.put(vertexArray[i+2]);
			}
			if (hasNormals) {
				vertices.put(normalArray[i]);
				vertices.put(normalArray[i+1]);
				vertices.put(normalArray[i+2]);
			}
			if (hasColors) {
				vertices.put(colorArray[i]);
				vertices.put(colorArray[i+1]);
				vertices.put(colorArray[i+2]);
			}
			if (hasTextureCoordinates) {
				vertices.put(textureCoordinateArray[i]);
				vertices.put(textureCoordinateArray[i+1]);
				vertices.put(textureCoordinateArray[i+2]);
			}
		}
		assert(vertices.position() == vertexArray.length/3);
		vertices.rewind();
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, 
				totalBufferByteCount, 
				vertices, 
				GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		
		initialized = true;
	}
	
	public void display(GL2GL3 gl) {
		if (! initialized)
			init(gl);
	}
	
	public void dispose(GL2GL3 gl2gl3) {
		GL gl = gl2gl3.getGL();
		if (vbo > 0) {
			int[] ix = {vbo};
			gl.glDeleteBuffers(1, ix, 0);
			vbo = 0;
		}
		initialized = false;
	}
}
