package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

public class MeshActor 
implements GLActor
{
    public enum Smoothing {
        SMOOTHING_ON,
        SMOOTHING_OFF
    };
    private Smoothing smoothing = Smoothing.SMOOTHING_ON;
    
    private PolygonalMesh mesh;
    private BoundingBox3d boundingBox;
    
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
        displayImmediateMode(gl);
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
    private void displayImmediateMode(GL2 gl) {
        for (PolygonalMesh.Face face : mesh.getFaces()) {
            // Paint
            gl.glBegin(GL2.GL_POLYGON);
            if ((smoothing == Smoothing.SMOOTHING_OFF) && (face.computedNormal != null))
                gl.glNormal3d(face.computedNormal.getX(), face.computedNormal.getY(), face.computedNormal.getZ());
            for (int v : face.vertexIndexes) {
                PolygonalMesh.Vertex vertex = mesh.getVertexes().get(v-1);
                if ((smoothing == Smoothing.SMOOTHING_ON) && (vertex.computedNormal != null))
                    gl.glNormal3d(vertex.computedNormal.getX(), vertex.computedNormal.getY(), vertex.computedNormal.getZ());
                gl.glVertex4d(vertex.getX(), vertex.getY(), vertex.getZ(), vertex.getW());
            }
            gl.glEnd();
        }
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
    }

}
