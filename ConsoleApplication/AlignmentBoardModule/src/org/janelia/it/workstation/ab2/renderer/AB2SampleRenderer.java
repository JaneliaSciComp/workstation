package org.janelia.it.workstation.ab2.renderer;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.opengl.GL4;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.Camera3DFollowBoxActor;
import org.janelia.it.workstation.ab2.actor.Image3DActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.model.AB2Image3D_RGBA8UI;
import org.janelia.it.workstation.ab2.shader.AB2ActorPickShader;
import org.janelia.it.workstation.ab2.shader.AB2ActorShader;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleRenderer extends AB23DRenderer {

    Logger logger= LoggerFactory.getLogger(AB2SampleRenderer.class);

    private Matrix4 modelMatrix;
    private BoundingBoxActor boundingBoxActor;
    private Camera3DFollowBoxActor cameraFollowBoxActor;

    private Image3DActor image3DActor;

    private GLShaderActionSequence drawShaderSequence;
    private GLShaderActionSequence basic3DShaderSequence;

    private GLShaderActionSequence pickShaderSequence;

    int actorCount=0;

    private int getNextActorIndex() { actorCount++; return actorCount; }

    public AB2SampleRenderer() {
        super();

        drawShaderSequence=new GLShaderActionSequence("DrawSequence");
        basic3DShaderSequence=new GLShaderActionSequence( "Basic3D");
        pickShaderSequence=new GLShaderActionSequence("PickSequence");

        drawShaderSequence.setShader(new AB2ActorShader());
        basic3DShaderSequence.setShader(new AB2Basic3DShader());
        pickShaderSequence.setShader(new AB2ActorPickShader());

        addDrawShaderActionSequence(drawShaderSequence);
        addDrawShaderActionSequence(basic3DShaderSequence);
        addPickShaderActionSequence(pickShaderSequence);
    }

    @Override
    public void init(GL4 gl) {
        logger.info("Starting init()");
        addBoundingBox();
        //addOriginPointActor();
        //addCameraFollowBoxActor();
        logger.info("drawShaderSequence containts "+drawShaderSequence.getActorSequence().size()+" actors");
        super.init(gl);
        logger.info("Finished init()");
        initialized=true;
    }

    public void clearActors() {
        clearActionSequenceActors(drawShaderSequence);
        clearActionSequenceActors(pickShaderSequence);
    }

    public void addSample3DImage(byte[] data) {
        clearImage3DActor();
        AB2Image3D_RGBA8UI image3d=createImage3dFromBytes(data);
        addImage3DActor(image3d);
    }

    private void clearImage3DActor() {
        // For some reason, the cast to GLAbstractActor is necessary for compatibility with the apache commons Pair implementation
        if (image3DActor!=null) {
            ImmutablePair<GLAbstractActor, GLShaderProgram> actorPair = new ImmutablePair<>((GLAbstractActor) image3DActor, drawShaderSequence.getShader());
            actorDisposalQueue.add(actorPair);
            image3DActor = null;
        }
    }

    static public AB2Image3D_RGBA8UI createImage3dFromBytes(byte[] data) {

        // First get dimensions
        byte[] dimBytes=new byte[12];
        for (int i=0;i<12;i++) { dimBytes[i]=data[i]; }
        IntBuffer intBuf = ByteBuffer.wrap(dimBytes).asIntBuffer();
        int[] dimArray = new int[intBuf.remaining()];
        intBuf.get(dimArray);

        // Populate
        int imageSize=dimArray[0] * dimArray[1] * dimArray[2] * 4;
        byte[] newData= Arrays.copyOfRange(data, 12, imageSize+12);
        AB2Image3D_RGBA8UI image3d=new AB2Image3D_RGBA8UI(dimArray[0], dimArray[1], dimArray[2], newData);
        data=null;
        return image3d;
    }

    private void addOriginPointActor() {
        List<Vector3> originPointList=new ArrayList<>();
        originPointList.add(new Vector3(0.5f, 0.5f, 0.5f));
        originPointList.add(new Vector3(0.5f, 0.5f, 0.5f));
        PointSetActor pointSetActor = new PointSetActor(this, getNextActorIndex(), originPointList);
        colorIdMap.put(pointSetActor.getActorId(), new Vector4(0f, 0f, 1f, 1f));
        drawShaderSequence.getActorSequence().add(pointSetActor);
    }

    private void addCameraFollowBoxActor() {
        cameraFollowBoxActor=new Camera3DFollowBoxActor(this, getNextActorIndex(), new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        colorIdMap.put(cameraFollowBoxActor.getActorId(), new Vector4(0.7f, 0.0f, 0.0f, 1.0f));
        drawShaderSequence.getActorSequence().add(cameraFollowBoxActor);
    }

    private void addImage3DActor(AB2Image3D_RGBA8UI image3d) {
        Vector3 v0=new Vector3(0f, 0f, 0f);
        Vector3 v1=new Vector3(1f, 1f, 1f);
        image3DActor=new Image3DActor(this, getNextActorIndex(), v0, v1, image3d.getXDim(), image3d.getYDim(), image3d.getZDim(), image3d.getData());
        drawShaderSequence.getActorSequence().add(image3DActor);
        ImmutablePair<GLAbstractActor, GLShaderProgram> actorPair = new ImmutablePair<>((GLAbstractActor) image3DActor, drawShaderSequence.getShader());
        actorInitQueue.add(actorPair);
    }

    private void addBoundingBox() {
        // Bounding Box
        boundingBoxActor=new BoundingBoxActor(this, getNextActorIndex(), new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        colorIdMap.put(boundingBoxActor.getActorId(), new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
        logger.info("addBoundingBox() adding boundingBoxActor");
        basic3DShaderSequence.getActorSequence().add(boundingBoxActor);
        logger.info("drawShaderSequence actor list size="+drawShaderSequence.getActorSequence().size());
    }

    public void setColorId(int styleId, Vector4 color) {
        colorIdMap.put(styleId, color);
    }

    public Vector4 getColorId(int styleId) {
        return colorIdMap.get(styleId);
    }

    public void reshape(GL4 gl, int x, int y, int width, int height) {
        //logger.info("reshape() x="+x+" y="+y+" width="+width+" height="+height);
        super.reshape(gl, x, y, width, height);
    }

    @Override
    public void dispose(GL4 gl) {
        super.dispose(gl);
        disposePickFramebuffer(gl);
    }

}
