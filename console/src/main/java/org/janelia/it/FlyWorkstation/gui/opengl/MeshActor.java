package org.janelia.it.FlyWorkstation.gui.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public class MeshActor 
implements GLActor
{
    public enum DisplayMethod {
        IMMEDIATE_MODE,
        DISPLAY_LISTS,
        VERTEX_BUFFER_OBJECTS,
    }
    
    private boolean smoothing = true;
    private PolygonalMesh mesh;
    private BoundingBox3d boundingBox;
    private DisplayMethod displayMethod = DisplayMethod.DISPLAY_LISTS;
    // display list render method
    private int displayList = 0;
    // vertex buffer object render method
    private int vertexVbo = 0;
    private int normalVbo = 0;
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
    public void display(GL2 gl) {
        if (displayMethod == DisplayMethod.IMMEDIATE_MODE)
            displayUsingImmediateMode(gl);
        else if (displayMethod == DisplayMethod.DISPLAY_LISTS)
            displayUsingDisplayList(gl); // should be faster than immediate
        else if (displayMethod == DisplayMethod.VERTEX_BUFFER_OBJECTS)
            displayUsingVertexBufferObjects(gl); // should be faster than immediate
        else
            throw new UnsupportedOperationException("Display mode not implemented yet: "+displayMethod);
    }
    
    /**
     * Easiest, most backwards compatible, slowest method.
     * 
     * TODO - more modern methods:
     *   display lists
     *   vertex arrays
     *   vertex buffer objects
     * 
     * @param gl OpenGL rendering context
     */
    private void displayUsingImmediateMode(GL2 gl) {
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
    
    private void displayUsingVertexBufferObjects(GL2 gl) {
        if (vertexVbo == 0) {
            // Initialize vertex buffer objects
            int[] vbos = {0,0,0};
            gl.glGenBuffers(3, vbos, 0);
            vertexVbo = vbos[0];
            normalVbo = vbos[1];
            indexVbo = vbos[2];
            //
            FloatBuffer vertices = ByteBuffer.allocateDirect(3 * 4 * mesh.getVertexes().size()).asFloatBuffer();
            vertices.rewind();
            for (PolygonalMesh.Vertex v : mesh.getVertexes()) {
                vertices.put((float)v.getX());
                vertices.put((float)v.getY());
                vertices.put((float)v.getZ());
            }
            // TODO
            // indexCount = ?;
        }
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVbo);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexVbo);
        gl.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    /**
     * Display lists are the old fashioned way to improve opengl performance
     * @param gl
     */
    private void displayUsingDisplayList(GL2 gl) {
        // The very first time, paint in immediate mode, and store a display list
        if (displayList < 1) {
            displayList = gl.glGenLists(1);
            gl.glNewList(displayList, GL2.GL_COMPILE);
            displayUsingImmediateMode(gl); // just this one time!
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
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
        if (displayList > 0) {
            gl.glDeleteLists(displayList, 1);
            displayList = 0;
        }
    }

}
