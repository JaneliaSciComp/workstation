package org.janelia.it.workstation.ab2.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    IntBuffer skeletonEdgeArrayId=IntBuffer.allocate(1);
    IntBuffer skeletonEdgeBufferId=IntBuffer.allocate(1);

    FloatBuffer skeletonEdgeFb;

    private Matrix4 modelMatrix;

    private BoundingBoxActor boundingBoxActor;
    private PointSetActor pointSetActor;
    private LineSetActor lineSetActor;

    Map<Integer, Vector4> styleIdMap=new HashMap<>();

    static final int BOUNDING_BOX_ID=1;
    static final int POINT_SET_ID=2;
    static final int LINE_SET_ID=3;

    public AB2SkeletonRenderer() {
        super(new AB2ActorShader());
        styleIdMap.put(BOUNDING_BOX_ID, new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
        styleIdMap.put(POINT_SET_ID, new Vector4(1.0f, 0.0f, 0.0f, 1.0f));
        styleIdMap.put(LINE_SET_ID, new Vector4(0.0f, 0.0f, 1.0f, 1.0f));
        shaderActionSequence.setShaderUpdateCallback(getShaderUpdateCallback());
        shaderActionSequence.setActorUpdateCallback(getActorSequenceUpdateCallback());
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
        List<Vector3> skeletonPoints=getSkeletonPointSet(skeleton);
        List<Vector3> skeletonLines=getSkeletonLineSet(skeleton);

        // Bounding Box
        boundingBoxActor=new BoundingBoxActor(BOUNDING_BOX_ID, new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        shaderActionSequence.getActorSequence().add(boundingBoxActor);

        pointSetActor=new PointSetActor(POINT_SET_ID, skeletonPoints);
        shaderActionSequence.getActorSequence().add(pointSetActor);

        lineSetActor=new LineSetActor(LINE_SET_ID, skeletonLines);
        shaderActionSequence.getActorSequence().add(lineSetActor);
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

    protected List<Vector3> getSkeletonPointSet(AB2NeuronSkeleton skeleton) {
        List<Vector3> pointList=new ArrayList<>();
        getSkeletonPointInfo(skeleton.getRootNode(), pointList);
        return pointList;
    }

    void getSkeletonPointInfo(AB2NeuronSkeleton.Node node, List<Vector3> pointList) {
        if (node==null) return;
        pointList.add(new Vector3((float)node.x(), (float)node.y(), (float)node.z()));
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            for (AB2NeuronSkeleton.Node child : children) {
                getSkeletonPointInfo(child, pointList);
            }
        }
    }

    protected List<Vector3> getSkeletonLineSet(AB2NeuronSkeleton skeleton) {
        List<Vector3> lineList=new ArrayList<>();
        getSkeletonLineInfo(skeleton.getRootNode(), lineList);
        return lineList;
    }

    void getSkeletonLineInfo(AB2NeuronSkeleton.Node node, List<Vector3> lineList) {
        if (node==null) return;
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            for (AB2NeuronSkeleton.Node child : children) {
                lineList.add(new Vector3((float)node.x(), (float)node.y(), (float)node.z()));
                lineList.add(new Vector3((float)child.x(), (float)child.y(), (float)child.z()));
                getSkeletonLineInfo(child, lineList);
            }
        }
    }

    @Override
    public void init(GL4 gl) {
        super.init(gl);
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

}
