package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL2;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public class MeshActor 
implements GLActor
{
    private boolean smoothing = true;
    private PolygonalMesh mesh;
    private BoundingBox3d boundingBox;
    private int displayList = 0;
    
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
        // displayUsingImmediateMode(gl);
        displayUsingDisplayList(gl); // should be faster than immediate
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
