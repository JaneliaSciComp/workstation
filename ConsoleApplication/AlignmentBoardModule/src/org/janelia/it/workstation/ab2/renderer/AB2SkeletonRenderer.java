package org.janelia.it.workstation.ab2.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.LineSetActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLActorUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderUpdateCallback;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.janelia.it.workstation.ab2.shader.AB2ActorShader;
import org.janelia.it.workstation.ab2.shader.AB2SkeletonShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonRenderer extends AB2Basic3DRenderer {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonRenderer.class);

    private Matrix4 modelMatrix;
    private BoundingBoxActor boundingBoxActor;

    private List<AB2NeuronSkeleton> skeletons;
    private List<PointSetActor> pointSetActors=new ArrayList<>();
    private List<LineSetActor> lineSetActors=new ArrayList<>();

    Map<Integer, Vector4> styleIdMap=new HashMap<>();

    //IntBuffer pickFramebufferId=IntBuffer.allocate(1);
    IntBuffer pickFramebufferId;
    IntBuffer pickColorTextureId;
    IntBuffer pickDepthTextureId;

    static final int BOUNDING_BOX_ID=1;

    public AB2SkeletonRenderer() {
        super(new AB2ActorShader());
        styleIdMap.put(BOUNDING_BOX_ID, new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
        shaderActionSequence.setShaderUpdateCallback(getShaderUpdateCallback());
        shaderActionSequence.setActorUpdateCallback(getActorSequenceUpdateCallback());

        // Bounding Box
        boundingBoxActor=new BoundingBoxActor(BOUNDING_BOX_ID, new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        shaderActionSequence.getActorSequence().add(boundingBoxActor);
    }


    public synchronized void setSkeletons(List<AB2NeuronSkeleton> skeletons) {
        this.skeletons=skeletons;
        updateSkeletons();
    }

    private void updateSkeletons() {
        logger.info("updateSkeletons() called");
        if (skeletons==null) {
            return;
        }
        Random random=new Random(new Date().getTime());
        for (int i=0;i<skeletons.size();i++) {
            List<Vector3> skeletonPoints = skeletons.get(i).getSkeletonPointSet();
            List<Vector3> skeletonLines = skeletons.get(i).getSkeletonLineSet();

            PointSetActor pointSetActor = new PointSetActor((i*2)+2, skeletonPoints);
            shaderActionSequence.getActorSequence().add(pointSetActor);
            styleIdMap.put(pointSetActor.getActorId(), new Vector4(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f));

            LineSetActor lineSetActor = new LineSetActor((i*2)+3, skeletonLines);
            shaderActionSequence.getActorSequence().add(lineSetActor);
            styleIdMap.put(lineSetActor.getActorId(), new Vector4(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f));
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

    public void reshape(GL4 gl, int x, int y, int width, int height) {
        resetPickFramebuffer(gl, width, height);
        super.reshape(gl, x, y, width, height);
        logger.info("reshape() x="+x+" y="+y+" width="+width+" height="+height);
    }

    protected void resetPickFramebuffer(GL4 gl, int width, int height) {
        disposePickFramebuffer(gl);

        pickFramebufferId=IntBuffer.allocate(1);
        gl.glGenFramebuffers(1, pickFramebufferId);
        gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, pickFramebufferId.get(0));

        pickColorTextureId=IntBuffer.allocate(1);
        gl.glGenTextures(1, pickColorTextureId);
        gl.glBindTexture(gl.GL_TEXTURE_2D, pickColorTextureId.get(0));
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8, width, height, 0, GL4.GL_BGRA, GL4.GL_UNSIGNED_BYTE, null);

        pickDepthTextureId=IntBuffer.allocate(1);
        gl.glGenTextures(1, pickDepthTextureId);
        gl.glBindTexture(gl.GL_TEXTURE_2D, pickDepthTextureId.get(0));
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_DEPTH_COMPONENT, width, height, 0, GL4.GL_DEPTH_COMPONENT, GL4.GL_FLOAT, null);

        gl.glFramebufferTexture(gl.GL_FRAMEBUFFER, gl.GL_COLOR_ATTACHMENT0, pickColorTextureId.get(0), 0);
        gl.glFramebufferTexture(gl.GL_FRAMEBUFFER, gl.GL_DEPTH_ATTACHMENT, pickDepthTextureId.get(0), 0);

        gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, 0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, 0);

        int status = gl.glCheckFramebufferStatus(GL4.GL_FRAMEBUFFER);
        if (status != GL4.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Failed to establish framebuffer: {}", decodeFramebufferStatus(status));
        }
        else {
            logger.info("Picking Framebuffer complete.");
        }
    }

    protected void disposePickFramebuffer(GL4 gl) {
        if (pickDepthTextureId!=null) {
            gl.glDeleteTextures(1, pickDepthTextureId);
            pickDepthTextureId=null;
        }
        if (pickColorTextureId!=null) {
            gl.glDeleteTextures(1, pickColorTextureId);
            pickColorTextureId=null;
        }
        if (pickFramebufferId!=null) {
            gl.glDeleteFramebuffers(1, pickFramebufferId);
            pickFramebufferId=null;
        }
    }

    @Override
    public void dispose(GL4 gl) {
        super.dispose(gl);
        disposePickFramebuffer(gl);
    }

    @Override
    protected GLShaderUpdateCallback getShaderUpdateCallback() {
        return new GLShaderUpdateCallback() {
            @Override
            public void update(GL4 gl, Object o) {

                AB2ActorShader actorShader = (AB2ActorShader) shader;
                actorShader.setMVP(gl, mvp);
                gl.glPointSize(3.0f);

            }
        };
    }

    @Override
    protected GLActorUpdateCallback getActorSequenceUpdateCallback() {
        return new GLActorUpdateCallback() {
            @Override
            public void update(GL4 gl, Object o) {

                GLAbstractActor actor = (GLAbstractActor)o;
                int actorId=actor.getActorId();
                Vector4 actorColor=styleIdMap.get(actorId);
                if (actorColor!=null) {
                    AB2ActorShader actorShader = (AB2ActorShader) shader;
                    actorShader.setStyleIdColor(gl, actorColor);
                    //logger.info("Set color id="+actorId+" to="+actorColor.toString());
                }
            }
        };
    }

    private String decodeFramebufferStatus( int status ) {
        String rtnVal = null;
        switch (status) {
            case GL4.GL_FRAMEBUFFER_UNDEFINED:
                rtnVal = "GL_FRAMEBUFFER_UNDEFINED means target is the default framebuffer, but the default framebuffer does not exist.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT :
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT is returned if any of the framebuffer attachment points are framebuffer incomplete.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT is returned if the framebuffer does not have at least one image attached to it.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER is returned if the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE\n" +
                        "		 is GL_NONE for any color attachment point(s) named by GL_DRAW_BUFFERi.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER is returned if GL_READ_BUFFER is not GL_NONE\n" +
                        "		 and the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE is GL_NONE for the color attachment point named\n" +
                        "		 by GL_READ_BUFFER.";
                break;
            case GL4.GL_FRAMEBUFFER_UNSUPPORTED:
                rtnVal = "GL_FRAMEBUFFER_UNSUPPORTED is returned if the combination of internal formats of the attached images violates\n" +
                        "		 an implementation-dependent set of restrictions.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE is returned if the value of GL_RENDERBUFFER_SAMPLES is not the same\n" +
                        "		 for all attached renderbuffers; if the value of GL_TEXTURE_SAMPLES is the not same for all attached textures; or, if the attached\n" +
                        "		 images are a mix of renderbuffers and textures, the value of GL_RENDERBUFFER_SAMPLES does not match the value of\n" +
                        "		 GL_TEXTURE_SAMPLES.  GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE  also returned if the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS is\n" +
                        "		 not the same for all attached textures; or, if the attached images are a mix of renderbuffers and textures, the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS\n" +
                        "		 is not GL_TRUE for all attached textures.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                rtnVal = " is returned if any framebuffer attachment is layered, and any populated attachment is not layered,\n" +
                        "		 or if all populated color attachments are not from textures of the same target.";
                break;
            default:
                rtnVal = "--Message not decoded: " + status;
        }
        return rtnVal;
    }

}
