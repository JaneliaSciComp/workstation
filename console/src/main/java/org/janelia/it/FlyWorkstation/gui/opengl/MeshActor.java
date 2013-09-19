package org.janelia.it.FlyWorkstation.gui.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
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
implements GLActor
{
    private static Logger logger = LoggerFactory.getLogger( MeshActor.class );
    protected static GLU glu = new GLU();
    private final int floatsPerVertex = 4;
    private final int floatsPerNormal = 3;
    private final int bytesPerFloat = 4;

    /**
     * Various rendering methodologies from simplest/oldest (IMMEDIATE_MODE)
     * to most modern (VERTEX_BUFFER_OBJECTS)
     * 
     * @author brunsc
     *
     */
    public enum DisplayMethod {
        IMMEDIATE_MODE,
        DISPLAY_LISTS,
        VERTEX_BUFFER_OBJECTS,
    }
    
    private boolean smoothing = true;
    private PolygonalMesh mesh;
    private BoundingBox3d boundingBox;
    private DisplayMethod displayMethod = DisplayMethod.VERTEX_BUFFER_OBJECTS;
    // display list render method
    private int displayList = 0;
    // vertex buffer object render method
    private int vertexNormalVbo = 0;
    private int indexVbo = 0;
    private int indexCount = 0;
    
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

    @Override
    public void display(GLAutoDrawable glDrawable) {
        if (displayMethod == DisplayMethod.IMMEDIATE_MODE)
            displayUsingImmediateMode(glDrawable);
        else if (displayMethod == DisplayMethod.DISPLAY_LISTS)
            displayUsingDisplayList(glDrawable); // should be faster than immediate
        else if (displayMethod == DisplayMethod.VERTEX_BUFFER_OBJECTS)
            displayUsingVertexBufferObjects(glDrawable); // should be faster than immediate
        else
            throw new UnsupportedOperationException("Display mode not implemented yet: "+displayMethod);
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
    private void displayUsingImmediateMode(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        for (PolygonalMesh.Face face : mesh.getFaces()) {
            // Paint
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            if ((!smoothing) && (face.computedNormal != null))
                gl.glNormal3d(face.computedNormal.getX(), face.computedNormal.getY(), face.computedNormal.getZ());
            for (int v : face.vertexIndexes) {
                PolygonalMesh.Vertex vertex = mesh.getVertexes().get(v-1);
                if (smoothing && (vertex.computedNormal != null))
                    gl.glNormal3d(vertex.computedNormal.getX(), vertex.computedNormal.getY(), vertex.computedNormal.getZ());
                gl.glVertex4d(vertex.getX(), vertex.getY(), vertex.getZ(), vertex.getW());
            }
            gl.glEnd();
        }
    }

    private void initializeVbos(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
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
        if (indices.position() != indexCount)
            System.err.println("arithmetic problem");
        indices.rewind();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, 
                totalIndexByteCount, 
                indices, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    private void displayUsingVertexBufferObjects(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        if (vertexNormalVbo < 1) // first time?
            initializeVbos(glDrawable);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

        final int bytesPerVertexNormal = (floatsPerVertex + floatsPerNormal)*bytesPerFloat;
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexNormalVbo);
        gl.glVertexPointer(floatsPerVertex, GL.GL_FLOAT, bytesPerVertexNormal, 0);
        gl.glNormalPointer(GL.GL_FLOAT, bytesPerVertexNormal, floatsPerVertex*bytesPerFloat);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
        gl.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);

        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        checkGlError(gl, "display mesh using vbos");
    }

    /**
     * Display lists are the old fashioned way to improve opengl performance
     * @param gl
     */
    private void displayUsingDisplayList(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        // The very first time, paint in immediate mode, and store a display list
        if (displayList < 1) {
            displayList = gl.glGenLists(1);
            gl.glNewList(displayList, GL2.GL_COMPILE);
            displayUsingImmediateMode(glDrawable); // just this one time!
            gl.glEndList();
        }
        // On subsequent renders, use the display list
        else {
            gl.glCallList(displayList);
        }
    }
    
    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }

    public boolean getSmoothing() {
        return smoothing;
    }

    public void setSmoothing(boolean smoothing) {
        this.smoothing = smoothing;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        if (displayList > 0) {
            gl.glDeleteLists(displayList, 1);
            displayList = 0;
        }
        if (vertexNormalVbo > 0) {
            int[] vbos = {vertexNormalVbo, indexVbo};
            gl.glDeleteBuffers(2, vbos, 0);
            vertexNormalVbo = 0;
            indexVbo = 0;
        }
    }

    private void checkGlError(GL gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );  
    }

}
