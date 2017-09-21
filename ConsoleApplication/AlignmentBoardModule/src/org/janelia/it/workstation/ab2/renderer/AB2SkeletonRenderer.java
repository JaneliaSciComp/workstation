package org.janelia.it.workstation.ab2.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.janelia.it.workstation.ab2.shader.AB2SkeletonShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonRenderer extends AB2Basic3DRenderer {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonRenderer.class);

    IntBuffer skeletonVertexArrayId=IntBuffer.allocate(1);
    IntBuffer skeletonVertexBufferId=IntBuffer.allocate(1);
    IntBuffer skeletonInterleavedBufferId=IntBuffer.allocate(1);

    IntBuffer boundaryVertexArrayId=IntBuffer.allocate(1);
    IntBuffer boundaryVertexBufferId=IntBuffer.allocate(1);
    IntBuffer boundaryInterleavedBufferId=IntBuffer.allocate(1);


    FloatBuffer skeletonInterleavedFb;
    FloatBuffer boundaryInterleavedFb;

    private Matrix4 modelMatrix;


    public AB2SkeletonRenderer() {
        super(new AB2SkeletonShader());
    }

    private AB2NeuronSkeleton skeleton;

    public synchronized void setSkeleton(AB2NeuronSkeleton skeleton) {
        this.skeleton=skeleton;
        updateSkeleton();
    }

    private void updateSkeleton() {
        logger.info("updateSkeleton() called");
        if (skeleton==null) {
            return;
        }
    }

    @Override
    protected Matrix4 getModelMatrix() {
        //logger.info("getModelMatrix()");
        if (modelMatrix==null) {
            logger.info("computing new Model matrix");
            Matrix4 translationMatrix = new Matrix4();
            translationMatrix.set(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 1.0f, 0.0f,
                    -0.5f, -0.5f, 0.0f, 1.0f);
            Matrix4 scaleMatrix = new Matrix4();
            scaleMatrix.set(
                    2.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 2.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 2.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f);
            modelMatrix=translationMatrix.multiply(scaleMatrix);
        }
        //logger.info("returning modelMatrix="+modelMatrix.toString());
        return modelMatrix;
    }

    int addNodeInfo(AB2NeuronSkeleton.Node node, float[] nodeXYZRGB, int i, Random random) {
        nodeXYZRGB[i++]=(float)node.x();
        nodeXYZRGB[i++]=(float)node.y();
        nodeXYZRGB[i++]=(float)node.z();
        nodeXYZRGB[i++]=random.nextFloat();
        nodeXYZRGB[i++]=random.nextFloat();
        nodeXYZRGB[i++]=random.nextFloat();
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            for (AB2NeuronSkeleton.Node child : children) {
                i=addNodeInfo(child, nodeXYZRGB, i, random);
            }
        }
        return i;
    }

    @Override
    public void init(GL4 gl) {
        super.init(gl);

        logger.info("init() called");

        //////////////////////////////////////////////////////////////////////////////////////////
        // Skeleton
        //////////////////////////////////////////////////////////////////////////////////////////

        int nodeCount=skeleton.getSize();
        float[] nodeXYZRGB = new float[nodeCount*6];
        Random random=new Random(new Date().getTime());
        AB2NeuronSkeleton.Node rootNode=skeleton.getRootNode();
        int nodesAdded=(addNodeInfo(rootNode, nodeXYZRGB, 0, random))/6;
        logger.info("Added "+nodesAdded+" skeleton nodes, nodeCount="+nodeCount);

        skeletonInterleavedFb=createGLFloatBuffer(nodeXYZRGB);

        gl.glGenVertexArrays(1, skeletonVertexArrayId);
        checkGlError(gl, "i1 glGenVertexArrays error");

        gl.glBindVertexArray(skeletonVertexArrayId.get(0));
        checkGlError(gl, "i2 glBindVertexArray error");

        gl.glGenBuffers(1, skeletonInterleavedBufferId);
        checkGlError(gl, "i3 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, skeletonInterleavedBufferId.get(0));
        checkGlError(gl, "i4 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, skeletonInterleavedFb.capacity() * 4, skeletonInterleavedFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 glBufferData error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        //////////////////////////////////////////////////////////////////////////////////////////
        // Boundary
        //////////////////////////////////////////////////////////////////////////////////////////

        float[] boundaryData = new float[] {
                0.0f, 0.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,

                0.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,

                1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 0.0f,  1.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 0.0f,  1.0f, 1.0f, 1.0f,

                0.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                0.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                1.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                0.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                0.0f, 0.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,

                1.0f, 0.0f, 0.0f,  1.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f
        };

        boundaryInterleavedFb=createGLFloatBuffer(boundaryData);

        gl.glGenVertexArrays(1, boundaryVertexArrayId);
        checkGlError(gl, "i6 glGenVertexArrays error");

        gl.glBindVertexArray(boundaryVertexArrayId.get(0));
        checkGlError(gl, "i7 glBindVertexArray error");

        gl.glGenBuffers(1, boundaryInterleavedBufferId);
        checkGlError(gl, "i8 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, boundaryInterleavedBufferId.get(0));
        checkGlError(gl, "i9 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, boundaryInterleavedFb.capacity() * 4, boundaryInterleavedFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i10 glBufferData error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);


        logger.info("init() finished");

    }

    @Override
    protected GLDisplayUpdateCallback getDisplayUpdateCallback() {
        return new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {

                //logger.info("update() start");

                AB2SkeletonShader skeletonShader = (AB2SkeletonShader) shader;
                skeletonShader.setMVP(gl, mvp);
                gl.glPointSize(3.0f);

                /////////////////////////////////////////////////////////////////////////////////////////
                // Skeleton
                /////////////////////////////////////////////////////////////////////////////////////////

                gl.glBindVertexArray(skeletonVertexArrayId.get(0));
                checkGlError(gl, "d1 glBindVertexArray() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, skeletonInterleavedBufferId.get(0));
                checkGlError(gl, "d2 glBindBuffer error");

                gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 24, 0);
                checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(0);
                checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

                gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 24, 12);
                checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(1);
                checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

                gl.glDrawArrays(GL4.GL_POINTS, 0, skeleton.getSize());
                checkGlError(gl, "d7 glDrawArrays() error");

                //gl.glDrawArrays(GL4.GL_LINES, 0, 8);
                //checkGlError(gl, "d8 glDrawArrays() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

                /////////////////////////////////////////////////////////////////////////////////////////
                // Boundary
                /////////////////////////////////////////////////////////////////////////////////////////

                gl.glBindVertexArray(boundaryVertexArrayId.get(0));
                checkGlError(gl, "d1 glBindVertexArray() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, boundaryInterleavedBufferId.get(0));
                checkGlError(gl, "d2 glBindBuffer error");

                gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 24, 0);
                checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(0);
                checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

                gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 24, 12);
                checkGlError(gl, "d3 glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(1);
                checkGlError(gl, "d4 glEnableVertexAttribArray 0 () error");

                gl.glDrawArrays(GL4.GL_LINES, 0, 24);
                checkGlError(gl, "d7 glDrawArrays() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

                //logger.info("update() finish");
            }
        };
    }


}
