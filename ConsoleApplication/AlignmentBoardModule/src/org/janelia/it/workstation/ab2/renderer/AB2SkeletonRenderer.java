package org.janelia.it.workstation.ab2.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL4;

import org.janelia.it.workstation.ab2.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.janelia.it.workstation.ab2.shader.AB2SimpleCubeShader;
import org.janelia.it.workstation.ab2.shader.AB2SkeletonShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonRenderer extends AB2Basic3DRenderer {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonRenderer.class);

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    IntBuffer interleavedBufferId=IntBuffer.allocate(1);

    FloatBuffer interleavedFb;


    public AB2SkeletonRenderer() {
        super(new AB2SkeletonShader());
    }

    private AB2NeuronSkeleton skeleton;

    public synchronized void setSkeleton(AB2NeuronSkeleton skeleton) {
        this.skeleton=skeleton;
        updateSkeleton();
    }

    private void updateSkeleton() {
        if (skeleton==null) {
            return;
        }
    }

    void addNodeInfo(AB2NeuronSkeleton.Node node, float[] nodeXYZRGB, int i, Random random) {
        nodeXYZRGB[i++]=(float)node.x();
        nodeXYZRGB[i++]=(float)node.y();
        nodeXYZRGB[i++]=(float)node.z();
        nodeXYZRGB[i++]=random.nextFloat();
        nodeXYZRGB[i++]=random.nextFloat();
        nodeXYZRGB[i++]=random.nextFloat();
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            for (AB2NeuronSkeleton.Node child : children) {
                addNodeInfo(child, nodeXYZRGB, i, random);
            }
        }
    }

    @Override
    public void init(GL4 gl) {
        super.init(gl);

        logger.info("init() called");

        int nodeCount=skeleton.getSize();
        float[] nodeXYZRGB = new float[nodeCount*6];
        Random random=new Random(new Date().getTime());
        AB2NeuronSkeleton.Node rootNode=skeleton.getRootNode();
        addNodeInfo(rootNode, nodeXYZRGB, 0, random);

        interleavedFb=createGLFloatBuffer(nodeXYZRGB);

        gl.glGenVertexArrays(1, vertexArrayId);
        checkGlError(gl, "i1 glGenVertexArrays error");

        gl.glBindVertexArray(vertexArrayId.get(0));
        checkGlError(gl, "i2 glBindVertexArray error");

        gl.glGenBuffers(1, interleavedBufferId);
        checkGlError(gl, "i3 glGenBuffers() error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, interleavedBufferId.get(0));
        checkGlError(gl, "i4 glBindBuffer error");

        gl.glBufferData(GL4.GL_ARRAY_BUFFER, interleavedFb.capacity() * 4, interleavedFb, GL4.GL_STATIC_DRAW);
        checkGlError(gl, "i5 glBufferData error");

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        logger.info("init() finished");

    }

    @Override
    protected GLDisplayUpdateCallback getDisplayUpdateCallback() {
        return new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {

                //logger.info("update() start");

                AB2SimpleCubeShader cubeShader = (AB2SimpleCubeShader) shader;
                cubeShader.setMVP(gl, mvp);
                gl.glPointSize(3.0f);

                gl.glBindVertexArray(vertexArrayId.get(0));
                checkGlError(gl, "d1 ArraySortShader glBindVertexArray() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, interleavedBufferId.get(0));
                checkGlError(gl, "d2 ArraySortShader glBindBuffer error");

                gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 24, 0);
                checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(0);
                checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 24, 12);
                checkGlError(gl, "d3 ArraySortShader glVertexAttribPointer 0 () error");

                gl.glEnableVertexAttribArray(1);
                checkGlError(gl, "d4 ArraySortShader glEnableVertexAttribArray 0 () error");

                gl.glDrawArrays(GL4.GL_POINTS, 0, skeleton.getSize());
                checkGlError(gl, "d7 ArraySortShader glDrawArrays() error");

                //gl.glDrawArrays(GL4.GL_LINES, 0, 8);
                //checkGlError(gl, "d8 ArraySortShader glDrawArrays() error");

                gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);


                //logger.info("update() finish");
            }
        };
    }


}
