package org.janelia.it.workstation.ab2.renderer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL4;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.Camera3DFollowBoxActor;
import org.janelia.it.workstation.ab2.actor.Image3DActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.actor.Voxel3DActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.model.AB2Image3D_RGBA8UI;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.janelia.it.workstation.ab2.shader.AB2Volume3DShader;
import org.janelia.it.workstation.ab2.shader.AB2Voxel3DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Main3DRenderer extends AB2Renderer3D {

    Logger logger= LoggerFactory.getLogger(AB2Main3DRenderer.class);

    //private Matrix4 modelMatrix;
    private Matrix4 prMatrix;
    private int[] voxel3DxyBounds=new int[] { 0, 0, 10000, 10000 };

    private BoundingBoxActor boundingBoxActor;
    private Camera3DFollowBoxActor cameraFollowBoxActor;
    private Image3DActor image3DActor;
    private Voxel3DActor voxel3DActor;

    //private GLShaderActionSequence drawShaderSequence;

    private GLShaderActionSequence basic3DShaderSequence;
    private GLShaderActionSequence volume3DShaderSequence;
    private GLShaderActionSequence voxel3DShaderSequence;

    private GLShaderActionSequence pickShaderSequence;
    private AB2Controller controller;

    //int actorCount=0;
    //private int getNextActorIndex() { actorCount++; return actorCount; }

    private int getNextActorIndex() {
        return controller.getNextPickIndex();
    }

    public AB2Main3DRenderer(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        super();
        controller=AB2Controller.getController();

        setSizeParameters(x, y, width, height, screenWidth, screenHeight);

//        drawShaderSequence=new GLShaderActionSequence("DrawSequence");

        basic3DShaderSequence=new GLShaderActionSequence( "Basic3D");
        volume3DShaderSequence=new GLShaderActionSequence("Volume3D");
        voxel3DShaderSequence=new GLShaderActionSequence( "Voxel3D");
        pickShaderSequence=new GLShaderActionSequence("PickSequence");

//        drawShaderSequence.setShader(new AB2ActorShader());

        basic3DShaderSequence.setShader(new AB2Basic3DShader());
        volume3DShaderSequence.setShader(new AB2Volume3DShader());
        voxel3DShaderSequence.setShader(new AB2Voxel3DShader());
        pickShaderSequence.setShader(new AB2PickShader());

//        addDrawShaderActionSequence(drawShaderSequence);

        addDrawShaderActionSequence(basic3DShaderSequence);
        addDrawShaderActionSequence(volume3DShaderSequence);
        addDrawShaderActionSequence(voxel3DShaderSequence);

        addPickShaderActionSequence(pickShaderSequence);
    }

    @Override
    public void init(GL4 gl) {
        logger.info("Starting init()");
        //addBoundingBox();
        //addOriginPointActor();
        //addCameraFollowBoxActor();
        //addVoxel3DActorTest();
        super.init(gl);
        logger.info("Finished init()");
        initialized=true;
    }

    public void addVoxel3DActorTest() {
        List<Vector3> vertexList=new ArrayList<>();
        List<Vector4> colorList=new ArrayList<>();

        int dimX=1000;
        int dimY=700;
        int dimZ=100;

        Random r=new Random(new Date().getTime());

        for (int i=0;i<10000;i++) {
            vertexList.add(new Vector3(r.nextFloat(), r.nextFloat(), r.nextFloat()));
            colorList.add(new Vector4(r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat()));
        }

        logger.info("Creating voxel3DActor");
        Voxel3DActor voxel3DActor = new Voxel3DActor(this, getNextActorIndex(), vertexList, colorList, dimX, dimY, dimZ);

        logger.info("Adding voxel3DActor to shader sequence");
        voxel3DShaderSequence.getActorSequence().add(voxel3DActor);
    }

    public void addVoxel3DActor(Voxel3DActor voxel3DActor) {
        if (this.voxel3DActor!=null) {
            clearActionSequenceActors(voxel3DShaderSequence);
            this.voxel3DActor=null;
        }
        if (this.prMatrix!=null) {
            logger.info("addVoxel3DActor - setting pr matrix");
            voxel3DActor.setPostProjectionMatrix(this.prMatrix);
        }
        if (this.voxel3DxyBounds!=null) {
            logger.info("addVoxel3DActor - setting xy bounds");
            voxel3DActor.setXYBounds(voxel3DxyBounds[0], voxel3DxyBounds[1], voxel3DxyBounds[2], voxel3DxyBounds[3]);
        }
        this.voxel3DActor=voxel3DActor;
        addActor(this.voxel3DActor, voxel3DShaderSequence);
    }

    public void clearActors() {
        clearActionSequenceActors(basic3DShaderSequence);
        clearActionSequenceActors(volume3DShaderSequence);
        clearActionSequenceActors(voxel3DShaderSequence);
        clearActionSequenceActors(pickShaderSequence);

        boundingBoxActor=null;
        cameraFollowBoxActor=null;
        image3DActor=null;
        voxel3DActor=null;
    }

    public void addSample3DImage(byte[] data) {
//        clearImage3DActor();
//        AB2Image3D_RGBA8UI image3d=createImage3dFromBytes(data);
//        addImage3DActor(image3d);

        clearVoxel3DActor();
        resetView();
        Voxel3DActor v=new Voxel3DActor(this, getNextActorIndex(), 0.05f, data);
        addVoxel3DActor(v);
    }

    private void clearImage3DActor() {
        // For some reason, the cast to GLAbstractActor is necessary for compatibility with the apache commons Pair implementation
        if (image3DActor!=null) {
            removeActor(image3DActor, volume3DShaderSequence);
            image3DActor = null;
        }
    }

    private void clearVoxel3DActor() {
        // For some reason, the cast to GLAbstractActor is necessary for compatibility with the apache commons Pair implementation
        if (voxel3DActor!=null) {
            removeActor(voxel3DActor, voxel3DShaderSequence);
            voxel3DActor = null;
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
        basic3DShaderSequence.getActorSequence().add(pointSetActor);
    }

    private void addCameraFollowBoxActor() {
        cameraFollowBoxActor=new Camera3DFollowBoxActor(this, getNextActorIndex(), new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        colorIdMap.put(cameraFollowBoxActor.getActorId(), new Vector4(0.7f, 0.0f, 0.0f, 1.0f));
        basic3DShaderSequence.getActorSequence().add(cameraFollowBoxActor);
    }

    private void addImage3DActor(AB2Image3D_RGBA8UI image3d) {
        Vector3 v0=new Vector3(0f, 0f, 0f);
        Vector3 v1=new Vector3(1f, 1f, 1f);
        image3DActor=new Image3DActor(this, getNextActorIndex(), v0, v1, image3d.getXDim(), image3d.getYDim(), image3d.getZDim(), image3d.getData());
        addActor(image3DActor, voxel3DShaderSequence);
    }

    private void addBoundingBox() {
        // Bounding Box
        boundingBoxActor=new BoundingBoxActor(this, getNextActorIndex(), new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        colorIdMap.put(boundingBoxActor.getActorId(), new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
        logger.info("addBoundingBox() adding boundingBoxActor");
        basic3DShaderSequence.getActorSequence().add(boundingBoxActor);
    }

    public void setColorId(int styleId, Vector4 color) {
        colorIdMap.put(styleId, color);
    }

    public Vector4 getColorId(int styleId) {
        return colorIdMap.get(styleId);
    }

    public void reshape(GL4 gl, int x, int y, int width, int height) {
        //logger.info("reshape() x="+x+" y="+y+" width="+width+" height="+height);
        super.reshape(gl, x, y, width, height, width, height);
    }

    // This is for testing
    public void setVoxel3DActorPostProjectionMatrix(Matrix4 prMatrix) {
        this.prMatrix=prMatrix;
        if (voxel3DActor!=null) {
            voxel3DActor.setPostProjectionMatrix(prMatrix);
            logger.info("voxel3DActor - pr matrix set");
        } else {
            logger.info("voxel3DActor is null - pr matrix not set");
        }
    }

    public void setVoxel3DxyBounds(int x0, int y0, int x1, int y1) {
        voxel3DxyBounds[0]=x0;
        voxel3DxyBounds[1]=y0;
        voxel3DxyBounds[2]=x1;
        voxel3DxyBounds[3]=y1;
        if (voxel3DActor!=null) {
            voxel3DActor.setXYBounds(voxel3DxyBounds[0], voxel3DxyBounds[1], voxel3DxyBounds[2], voxel3DxyBounds[3]);
            logger.info("voxel3DActor - xy bounds set");
        } else {
            logger.info("voxel3DActor is null - xy bounds not set");
        }
    }

    @Override
    public void dispose(GL4 gl) {
        super.dispose(gl);
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        super.reshape(gl, x, y, width, height, screenWidth, screenHeight);
        setSizeParameters(x, y, width, height, screenWidth, screenHeight);
    }

    private void setSizeParameters(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        float[] parameters = computeOffsetParameters(x, y, width, height, screenWidth, screenHeight);
        setVoxel3DActorPostProjectionMatrix(getOffsetPostProjectionMatrix(parameters[0], parameters[1], parameters[2]));
        int[] xyBounds = getXYBounds(x, y, width, height);
        setVoxel3DxyBounds(xyBounds[0], xyBounds[1], xyBounds[2], xyBounds[3]);
    }

}
