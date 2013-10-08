package org.janelia.it.FlyWorkstation.gui.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

public class VertexBuffer {
	private final boolean hasVertices = true;
	private boolean hasNormals = false;
	private boolean hasColors = false;
	private boolean hasTextureCoordinates = false;

	private int vbo = 0;
	private int indexVbo = 0;
	private boolean initialized = false;

	// semantic values for computing byte array sizes
	private final int floatsPerNormal = 3;
	private final int floatsPerVertex = 3;
	private final int floatsPerColor = 3;
	private final int floatsPerTextureCoordinate = 3;
	private final int bytesPerFloat = Float.SIZE/8;

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
		
		int[] ix = {0,0};
		gl.glGenBuffers(2, ix, 0);
		vbo = ix[0];
		indexVbo = ix[1];

		// Compute backed byte offset
		int vertexByteCount = 0;
		int byteOffset = 0;
		if (hasVertices) {
			int db = bytesPerFloat * floatsPerVertex;
			vertexByteCount += db;
			byteOffset += db;
		}
		if (hasNormals) {
			normalByteOffset = byteOffset;
			int db = bytesPerFloat * floatsPerNormal;
			vertexByteCount += db;
			byteOffset += db;
		}
		if (hasColors) {
			colorByteOffset = byteOffset;
			int db = bytesPerFloat * floatsPerColor;
			vertexByteCount += db;
			byteOffset += db;
		}
		if (hasTextureCoordinates) {
			textureCoordinateByteOffset = byteOffset;
			int db = bytesPerFloat * floatsPerTextureCoordinate;
			vertexByteCount += db;
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
	
	public void display(GL2 gl2) {
		if (! initialized)
			init(gl2);
	}
	
	public void dispose(GL2GL3 gl2gl3) {
		GL gl = gl2gl3.getGL();
		if (vbo > 0) {
			int[] ix = {vbo, indexVbo};
			gl.glDeleteBuffers(2, ix, 0);
			vbo = 0;
		}
		initialized = false;
	}
}
