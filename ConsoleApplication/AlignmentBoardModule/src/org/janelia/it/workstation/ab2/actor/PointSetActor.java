package org.janelia.it.workstation.ab2.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointSetActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(PointSetActor.class);

    List<Vector3> points;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);

    FloatBuffer pointVertexFb;

    public PointSetActor(int actorId, List<Vector3> points) {
        this.actorId=actorId;
        this.points=points;
    }

    @Override
    public void init(GL4 gl) {

        int nodeCount=skeleton.getSize();
        float[] nodeXYZRGB = new float[nodeCount*6];
        Random random=new Random(new Date().getTime());
        AB2NeuronSkeleton.Node rootNode=skeleton.getRootNode();

        int nodeVerticesAdded=(addNodeInfo(rootNode, nodeXYZRGB, 0, random))/6;
        logger.info("Added "+nodeVerticesAdded+" skeleton node vertices, nodeCount="+nodeCount);

        int edgeVerticesAdded=(addEdgeInfo(rootNode, edgeXYZRGB, 0, random))/6;
        logger.info("Added "+edgeVerticesAdded+" skeleton edge vertices, nodeCount="+nodeCount);

        /// VERTICES

        skeletonVertexFb= GLAbstractActor.createGLFloatBuffer(nodeXYZRGB);

        gl.glGenVertexArrays(1, skeletonVertexArrayId);
        checkGlError(gl, "i1 glGenVertexArrays error");

        gl.glBindVertexArray(skeletonVertexArrayId.get(0));
        checkGlError(gl, "i2 glBindVertexArray error");

        gl.glGenBuffers(1, skeletonVertexBufferId);
        checkGlError(gl, "i3 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, skeletonVertexBufferId.get(0));
        checkGlError(gl, "i4 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, skeletonVertexFb.capacity() * 4, skeletonVertexFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 glBufferData error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        /// EDGES



    }

    @Override
    public void display(GL4 gl) {


    }

    @Override
    public void dispose(GL4 gl) {
        gl.glDeleteVertexArrays(1, boundaryVertexArrayId);
        gl.glDeleteBuffers(1, boundaryVertexBufferId);
    }



}
