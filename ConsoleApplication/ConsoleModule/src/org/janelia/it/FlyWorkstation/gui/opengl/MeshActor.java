package org.janelia.it.FlyWorkstation.gui.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opaque displayable object composed of a polygonal mesh,
 * just like most of the rest of the OpenGL applications in the
 * world use.
 * 
 * @author brunsc
 *
 */
public class MeshActor 
implements GL3Actor
{
    private static Logger logger = LoggerFactory.getLogger( MeshActor.class );
    protected static GLU glu = new GLU();
    private final int floatsPerVertex = 4;
    private final int floatsPerNormal = 3;
    private final int bytesPerFloat = 4;
    private int vertexLocation = 0; // shader program uniform index
    private int normalLocation = 1;

    /**
     * Various rendering methodologies from simplest/oldest (IMMEDIATE_MODE)
     * to most modern (VERTEX_BUFFER_OBJECTS)
     * 
     * @author brunsc
     *
     */
    public enum DisplayMethod {
        GL2_IMMEDIATE_MODE,
        GL2_DISPLAY_LISTS,
        GL2_VERTEX_BUFFER_OBJECTS,
        VBO_WITH_SHADER,
    }
    
    private boolean smoothing = true;
    private PolygonalMesh mesh;
    private BoundingBox3d boundingBox;
    private DisplayMethod displayMethod 
        // 	= DisplayMethod.GL2_VERTEX_BUFFER_OBJECTS; // GL2 only...
    	//  = DisplayMethod.GL2_DISPLAY_LISTS;
    	 = DisplayMethod.VBO_WITH_SHADER;
    // display list render method
    private int displayList = 0;
    // vertex buffer object render method
    private int vertexNormalVbo = 0;
    private int indexVbo = 0;
    private int indexCount = 0;
    private int vertexArrayObject = 0;
    
    public MeshActor(PolygonalMesh mesh) {
        this.mesh = mesh;
        mesh.computeFaceNormals();
        mesh.computeVertexNormals();
        // Compute bounding box
        BoundingBox3d bb = new BoundingBox3d();
        for (PolygonalMesh.Vertex v : mesh.getVertexes()) {
            Vec3 p = new Vec3(
                    v.getX()/v.getW(),
                    v.getY()/v.getW(),
                    v.getZ()/v.getW());
            bb.include(p);
        }
        this.boundingBox = bb;
    }

    private void displayGL2(GL2 gl2) {
        if (displayMethod == DisplayMethod.GL2_IMMEDIATE_MODE)
            displayUsingImmediateMode(gl2);
        else if (displayMethod == DisplayMethod.GL2_DISPLAY_LISTS)
            displayUsingDisplayList(gl2); // should be faster than immediate
        else if (displayMethod == DisplayMethod.GL2_VERTEX_BUFFER_OBJECTS)
            displayUsingVertexBufferObjects(gl2); // should be faster than immediate
        else if (displayMethod == DisplayMethod.VBO_WITH_SHADER)
            displayUsingVertexBufferObjectsWithShader(gl2); // should be faster than immediate
        else
            throw new UnsupportedOperationException("Display mode not implemented yet: "+displayMethod);
    }
    
    private void displayGL2GL3(GL2GL3 gl2gl3) {
        displayUsingVertexBufferObjectsWithShader(gl2gl3);
    }
    
    /**
     * Easiest, most backwards compatible, slowest method.
     * 
     * more modern methods:
     *   display lists
     *   vertex arrays
     *   vertex buffer objects
     * 
     * @param gl OpenGL rendering context
     */
    private void displayUsingImmediateMode(GL2 gl2) {
        checkGlError(gl2, "display mesh using immediate mode 0");
    	gl2.glEnable(GL2.GL_LIGHTING);
        for (PolygonalMesh.Face face : mesh.getFaces()) {
            // Paint
            gl2.glBegin(GL2.GL_TRIANGLE_FAN);
            if ((!smoothing) && (face.computedNormal != null))
                gl2.glNormal3f((float)face.computedNormal.getX(), (float)face.computedNormal.getY(), (float)face.computedNormal.getZ());
            for (int v : face.vertexIndexes) {
                PolygonalMesh.Vertex vertex = mesh.getVertexes().get(v-1);
                if (smoothing && (vertex.computedNormal != null))
                    gl2.glNormal3f((float)vertex.computedNormal.getX(), (float)vertex.computedNormal.getY(), (float)vertex.computedNormal.getZ());
                gl2.glVertex4d(vertex.getX(), vertex.getY(), vertex.getZ(), vertex.getW());
            }
            gl2.glEnd();
        }
        checkGlError(gl2, "display mesh using immediate mode 1");
    }

    private void initializeVbos(GL2GL3 gl2gl3) {
        GL gl = gl2gl3.getGL();
        checkGlError(gl, "initialize vbos 130");
        // Initialize vertex buffer objects
        int[] vbos = {0,0};
        gl.glGenBuffers(2, vbos, 0);
        indexVbo = vbos[1];
        // vertices
        // Improve data locality by packing vertex position with vertex normal
        vertexNormalVbo = vbos[0];
        checkGlError(gl, "create buffer handles");
        final int floatsPerNormal = 3;
        long totalVertexNormalByteCount = 
                (floatsPerVertex + floatsPerNormal) 
                * bytesPerFloat 
                * mesh.getVertexes().size();
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(
                (int)totalVertexNormalByteCount);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer vertices = vertexByteBuffer.asFloatBuffer();
        vertices.rewind();
        for (PolygonalMesh.Vertex v : mesh.getVertexes()) {
            // vertex
            vertices.put((float)v.getX());
            vertices.put((float)v.getY());
            vertices.put((float)v.getZ());
            if (floatsPerVertex >= 4)
                vertices.put((float)v.getW());
            // normal
            vertices.put((float)v.computedNormal.getX());
            vertices.put((float)v.computedNormal.getY());
            vertices.put((float)v.computedNormal.getZ());
        }
        vertices.rewind();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexNormalVbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, 
                totalVertexNormalByteCount, 
                vertices, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        checkGlError(gl, "initialize vbos 167");
        // indices
        int triangleCount = 0;
        for (PolygonalMesh.Face f : mesh.getFaces())
            triangleCount += f.vertexIndexes.size() - 2;
        indexCount = triangleCount * 3;
        long totalIndexByteCount = indexCount * Integer.SIZE/8;
        ByteBuffer indexByteBuffer = ByteBuffer.allocateDirect(
                (int)totalIndexByteCount);
        indexByteBuffer.order(ByteOrder.nativeOrder());
        IntBuffer indices = indexByteBuffer.asIntBuffer();
        indices.rewind();
        for (PolygonalMesh.Face f : mesh.getFaces()) {
            int v0 = f.vertexIndexes.get(0) - 1;
            // One triangle for every point after number 2
            for (int t = 0; t < f.vertexIndexes.size() - 2; ++t) {
                int v1 = f.vertexIndexes.get(t+1) - 1;
                int v2 = f.vertexIndexes.get(t+2) - 1;
                indices.put(v0);
                indices.put(v1);
                indices.put(v2);
            }
        }
        checkGlError(gl, "initialize vbos 190");
        if (indices.position() != indexCount)
            System.err.println("arithmetic problem");
        indices.rewind();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
        checkGlError(gl, "initialize vbos 195");
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, 
                totalIndexByteCount, 
                indices, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
        // Vertex array object
            
        int ix[] = {0};
        checkGlError(gl, "initialize vbos 203");
        gl2gl3.glGenVertexArrays(1, ix, 0);
        checkGlError(gl, "initialize vbos 205");
        vertexArrayObject = ix[0];
    }
    
    private void displayUsingVertexBufferObjects(GL2 gl2) {
        GL gl = gl2.getGL();
        checkGlError(gl, "display mesh using vbos 205");
        GL2GL3 gl2gl3 = gl2.getGL2GL3();
        // GL gl = glDrawable.getGL();
        // GL2 gl2 = gl.getGL2();
        checkGlError(gl, "display mesh using vbos 209");
        if (vertexNormalVbo < 1) // first time?
            initializeVbos(gl2);
        checkGlError(gl, "display mesh using vbos 211");
        gl2gl3.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2gl3.glEnableClientState(GL2.GL_NORMAL_ARRAY);

        checkGlError(gl, "display mesh using vbos 214");
        final int bytesPerVertexNormal = (floatsPerVertex + floatsPerNormal)*bytesPerFloat;
        gl2gl3.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexNormalVbo);
        gl2.glVertexPointer(floatsPerVertex, GL.GL_FLOAT, bytesPerVertexNormal, 0);
        gl2.glNormalPointer(GL.GL_FLOAT, bytesPerVertexNormal, floatsPerVertex*bytesPerFloat);
        gl2gl3.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexVbo);

        gl2.glEnable(GL2.GL_LIGHTING);
        gl2gl3.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);

        gl2gl3.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl2gl3.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        checkGlError(gl, "display mesh using vbos");
    }

    private void displayUsingVertexBufferObjectsWithShader(GL2GL3 gl2gl3) {
        GL gl = gl2gl3.getGL();
        checkGlError(gl, "display mesh using vbos and shader 0");
        // GL2 gl2 = gl.getGL2();
        if (vertexNormalVbo < 1) // first time?
            initializeVbos(gl2gl3);
        gl2gl3.glBindVertexArray(vertexArrayObject);
        
        final int bytesPerVertexNormal = (floatsPerVertex + floatsPerNormal)*bytesPerFloat;
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexNormalVbo);

        checkGlError(gl, "display mesh using vbos and shader 1");
        
        // vertices
        gl2gl3.glEnableVertexAttribArray(vertexLocation);

        gl2gl3.glVertexAttribPointer(vertexLocation, floatsPerVertex, GL.GL_FLOAT, false, bytesPerVertexNormal, 0);

        // normals
        // Normal might be compiled away if not used...
        if (normalLocation >= 0) {
            gl2gl3.glEnableVertexAttribArray(normalLocation);
            checkGlError(gl, "display mesh using vbos and shader 2");
            gl2gl3.glVertexAttribPointer(normalLocation, floatsPerNormal, GL.GL_FLOAT, false, bytesPerVertexNormal, floatsPerVertex*bytesPerFloat);
        }

        checkGlError(gl, "display mesh using vbos and shader 3");
        
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
        gl.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);

        checkGlError(gl, "display mesh using vbos and shader 4");

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    	gl2gl3.glBindVertexArray(0);
    }

    /**
     * Display lists are the old fashioned way to improve opengl performance
     * @param gl
     */
    private void displayUsingDisplayList(GL2 gl2) {
        // The very first time, paint in immediate mode, and store a display list
        if (displayList < 1) {
            displayList = gl2.glGenLists(1);
            gl2.glNewList(displayList, GL2.GL_COMPILE);
            displayUsingImmediateMode(gl2); // just this one time!
            gl2.glEndList();
        }
        // On subsequent renders, use the display list
        else {
            gl2.glCallList(displayList);
        }
    }
    
    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }

    public boolean getSmoothing() {
        return smoothing;
    }

    public void setVertexLocation(int vertexLocation) {
        this.vertexLocation = vertexLocation;
    }

    public void setNormalLocation(int normalLocation) {
        this.normalLocation = normalLocation;
    }

    public void setSmoothing(boolean smoothing) {
        this.smoothing = smoothing;
    }

    private void checkGlError(GL gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );  
    }

    @Override
    public void display(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        if (gl.isGL2()) {
            displayGL2(gl.getGL2());
        }
        else if (gl.isGL2GL3()) {
            displayGL2GL3(gl.getGL2GL3());
        }
    }

    @Override
    public void init(GLActorContext context) {
    }

    @Override
    public void dispose(GLActorContext context) {
        GL gl = context.getGLAutoDrawable().getGL();
        if (displayList > 0) {
            GL2 gl2 = gl.getGL2();
            gl2.glDeleteLists(displayList, 1);
            displayList = 0;
        }
        if (vertexNormalVbo > 0) {
            int[] vbos = {vertexNormalVbo, indexVbo};
            gl.glDeleteBuffers(2, vbos, 0);
            vertexNormalVbo = 0;
            indexVbo = 0;
        }
        if (vertexArrayObject > 0) {
            GL2GL3 gl2gl3 = gl.getGL2GL3();
            int[] ix = {vertexArrayObject};
            gl2gl3.glDeleteVertexArrays(1, ix, 0);
            vertexArrayObject = 0;
        }
    }

	public DisplayMethod getDisplayMethod() {
		return displayMethod;
	}

}
