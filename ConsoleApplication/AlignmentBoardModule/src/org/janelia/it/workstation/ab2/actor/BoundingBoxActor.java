package org.janelia.it.workstation.ab2.actor;

import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoundingBoxActor extends GLAbstractActor
{
    private final Logger logger = LoggerFactory.getLogger(BoundingBoxActor.class);

    Vector3 v0;
    Vector3 v1;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);

    public BoundingBoxActor() {
        v0=new Vector3(0.0f, 0.0f, 0.0f);
        v1=new Vector3(1.0f, 1.0f, 1.0f);
    }

    public void setBoundingBox(Vector3 v0, Vector3 v1) {
        this.v0=v0;
        this.v1=v1;
    }


    @Override
    public void display(GL4 gl) {
        super.display(gl);
    }

    @Override
    public void init(GL4 gl) {

    }

    @Override
    public void dispose(GL4 gl) {
        gl.glDeleteVertexArrays(1, vertexArrayId);
        gl.glDeleteBuffers(1, vertexBufferId);
    }

}
