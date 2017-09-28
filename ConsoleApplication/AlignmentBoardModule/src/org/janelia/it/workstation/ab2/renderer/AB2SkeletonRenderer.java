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
import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.LineSetActor;
import org.janelia.it.workstation.ab2.actor.PickSquareActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLActorUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderUpdateCallback;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.janelia.it.workstation.ab2.shader.AB2ActorPickShader;
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

    private PickSquareActor pickSquareActor;

    Map<Integer, Vector4> styleIdMap=new HashMap<>();

    //static final int BOUNDING_BOX_ID=1;

    int actorCount=0;

    private int getNextActorIndex() { actorCount++; return actorCount; }

    public AB2SkeletonRenderer() {
        super(new AB2ActorShader(), new AB2ActorPickShader());
        styleIdMap.put(getNextActorIndex(), new Vector4(1.0f, 1.0f, 1.0f, 1.0f));

        drawActionSequence.setShaderUpdateCallback(getDrawShaderUpdateCallback());
        drawActionSequence.setActorUpdateCallback(getActorSequenceDrawUpdateCallback());

        pickActionSequence.setShaderUpdateCallback(getPickShaderUpdateCallback());
        pickActionSequence.setActorUpdateCallback(getActorSequencePickUpdateCallback());


        // Bounding Box
        boundingBoxActor=new BoundingBoxActor(actorCount, new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        drawActionSequence.getActorSequence().add(boundingBoxActor);

        // Pick Square
        pickSquareActor=new PickSquareActor(getNextActorIndex(), new Vector2(0.4f, 0.4f), new Vector2(0.5f, 0.5f),
                new Vector4(1f, 0f, 0f, 1f), new Vector4(0f, 1f, 0f, 1f));
        styleIdMap.put(pickSquareActor.getActorId(), pickSquareActor.getColor0());
        drawActionSequence.getActorSequence().add(pickSquareActor);
        pickActionSequence.getActorSequence().add(pickSquareActor);
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

            PointSetActor pointSetActor = new PointSetActor(getNextActorIndex(), skeletonPoints);
            drawActionSequence.getActorSequence().add(pointSetActor);
            styleIdMap.put(pointSetActor.getActorId(), new Vector4(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f));

            LineSetActor lineSetActor = new LineSetActor(getNextActorIndex(), skeletonLines);
            drawActionSequence.getActorSequence().add(lineSetActor);
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
        logger.info("reshape() x="+x+" y="+y+" width="+width+" height="+height);
        super.reshape(gl, x, y, width, height);
    }

    @Override
    public void dispose(GL4 gl) {
        super.dispose(gl);
        disposePickFramebuffer(gl);
    }

    @Override
    protected GLShaderUpdateCallback getDrawShaderUpdateCallback() {
        return new GLShaderUpdateCallback() {
            @Override
            public void update(GL4 gl, Object o) {

                AB2ActorShader actorShader = (AB2ActorShader) drawShader;
                //logger.info("Check1");
                actorShader.setMVP(gl, mvp);
                //logger.info("Check1.1");
                gl.glPointSize(3.0f);

            }
        };
    }

    @Override
    protected GLActorUpdateCallback getActorSequenceDrawUpdateCallback() {
        return new GLActorUpdateCallback() {
            @Override
            public void update(GL4 gl, Object o) {

                GLAbstractActor actor = (GLAbstractActor)o;
                int actorId=actor.getActorId();
                Vector4 actorColor=styleIdMap.get(actorId);
                if (actorColor!=null) {
                    AB2ActorShader actorShader = (AB2ActorShader) drawShader;
                    actorShader.setStyleIdColor(gl, actorColor);
                    if (actor instanceof PickSquareActor) {
                        actorShader.setTwoDimensional(gl, true);
                    } else {
                        actorShader.setTwoDimensional(gl, false);
                    }
                    //logger.info("Set color id="+actorId+" to="+actorColor.toString());
                }
            }
        };
    }

    @Override
    protected GLShaderUpdateCallback getPickShaderUpdateCallback() {
        return new GLShaderUpdateCallback() {
            @Override
            public void update(GL4 gl, Object o) {

                AB2ActorPickShader actorShader = (AB2ActorPickShader) pickShader;
                //logger.info("Check2");
                actorShader.setMVP(gl, mvp);
                //logger.info("Check2.1");
                gl.glPointSize(3.0f);

            }
        };
    }

    @Override
    protected GLActorUpdateCallback getActorSequencePickUpdateCallback() {
        return new GLActorUpdateCallback() {
            @Override
            public void update(GL4 gl, Object o) {
                GLAbstractActor actor = (GLAbstractActor)o;
                AB2ActorPickShader actorPickShader=(AB2ActorPickShader) pickShader;
                if (actor instanceof PickSquareActor) {
                    int pickIndex=((PickSquareActor)actor).getPickIndex();
                    logger.info("getActorSequencePickUpdateCallback() - setting pickIndex="+pickIndex);
                    actorPickShader.setPickId(gl, pickIndex);
                    logger.info("getActorSequencePickUpdateCallback() - setting twoDimensional=true");
                    actorPickShader.setTwoDimensional(gl, true);
                } else {
                    actorPickShader.setTwoDimensional(gl, false);
                }
            }
        };
    }

}
