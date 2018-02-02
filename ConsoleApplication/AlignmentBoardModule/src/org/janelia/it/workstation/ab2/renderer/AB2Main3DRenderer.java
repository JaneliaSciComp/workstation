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
import org.janelia.it.workstation.ab2.AB2Properties;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.Camera3DFollowBoxActor;
import org.janelia.it.workstation.ab2.actor.Image3DActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.actor.Voxel3DActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.controller.AB2UserContext;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2Main3DRendererSetRangeEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseDragEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.model.AB2Image3D_RGBA8UI;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;
import org.janelia.it.workstation.ab2.shader.AB2Volume3DShader;
import org.janelia.it.workstation.ab2.shader.AB2Voxel3DShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Main3DRenderer extends AB2Renderer3D {

    Logger logger= LoggerFactory.getLogger(AB2Main3DRenderer.class);

    private Matrix4 prMatrix;
    private int[] voxel3DxyBounds=new int[] { 0, 0, 10000, 10000 };

    private Voxel3DActor voxel3DActor;

    private GLShaderActionSequence voxel3DShaderSequence;
    private GLShaderActionSequence pickShaderSequence;

    private AB2Controller controller;

    int x;
    int y;
    int width;
    int height;
    int screenWidth;
    int screenHeight;

    private int getNextActorIndex() {
        return controller.getNextPickIndex();
    }

    public AB2Main3DRenderer(int x, int y, int width, int height, int screenWidth, int screenHeight, GLRegion parentRegion) {
        super(parentRegion);

        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenHeight=screenHeight;
        this.screenWidth=screenWidth;

        controller=AB2Controller.getController();

        setVoxel3DActorSizeParameters(x, y, width, height, screenWidth, screenHeight);

        voxel3DShaderSequence=new GLShaderActionSequence( "Voxel3D");
        pickShaderSequence=new GLShaderActionSequence("PickSequence");

        voxel3DShaderSequence.setShader(new AB2Voxel3DShader());
        pickShaderSequence.setShader(new AB2PickShader());

        addDrawShaderActionSequence(voxel3DShaderSequence);
        addPickShaderActionSequence(pickShaderSequence);
    }

    @Override
    public void init(GL4 gl) {
        super.init(gl);
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

        Voxel3DActor voxel3DActor = new Voxel3DActor(this, getNextActorIndex(), vertexList, colorList, dimX, dimY, dimZ);

        voxel3DShaderSequence.getActorSequence().add(voxel3DActor);
    }

    public void addVoxel3DActor(Voxel3DActor voxel3DActor) {
        if (this.voxel3DActor!=null) {
            clearActionSequenceActors(voxel3DShaderSequence);
            this.voxel3DActor=null;
        }
        if (this.prMatrix!=null) {
            voxel3DActor.setPostProjectionMatrix(this.prMatrix);
        }
        if (this.voxel3DxyBounds!=null) {
            voxel3DActor.setXYBounds(voxel3DxyBounds[0], voxel3DxyBounds[1], voxel3DxyBounds[2], voxel3DxyBounds[3]);
        }
        this.voxel3DActor=voxel3DActor;
        addActor(this.voxel3DActor, voxel3DShaderSequence);
    }

    public void clearActors() {
        clearActionSequenceActors(voxel3DShaderSequence);
        clearActionSequenceActors(pickShaderSequence);
        voxel3DActor=null;
    }

    public void addSample3DImage(byte[] data) {
        clearVoxel3DActor();
        resetView();
        Voxel3DActor v=new Voxel3DActor(this, getNextActorIndex(), 0.0275f, data, true);
        addVoxel3DActor(v);
    }

    private void clearVoxel3DActor() {
        // For some reason, the cast to GLAbstractActor is necessary for compatibility with the apache commons Pair implementation
        if (voxel3DActor!=null) {
            removeActor(voxel3DActor, voxel3DShaderSequence);
            controller.setNeedsRepaint(true);
            try { Thread.sleep(100); } catch (Exception ex) {}
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

    // The public access is for testing
    public void setVoxel3DActorPostProjectionMatrix(Matrix4 prMatrix) {
        this.prMatrix=prMatrix;
        if (voxel3DActor!=null) {
            voxel3DActor.setPostProjectionMatrix(prMatrix);
        }
    }

    // The public access is for testing
    public void setVoxel3DxyBounds(int x0, int y0, int x1, int y1) {
        voxel3DxyBounds[0]=x0;
        voxel3DxyBounds[1]=y0;
        voxel3DxyBounds[2]=x1;
        voxel3DxyBounds[3]=y1;
        if (voxel3DActor!=null) {
            voxel3DActor.setXYBounds(voxel3DxyBounds[0], voxel3DxyBounds[1], voxel3DxyBounds[2], voxel3DxyBounds[3]);
        }
    }

    @Override
    public void dispose(GL4 gl) {
        super.dispose(gl);
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {

        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenHeight=screenHeight;
        this.screenWidth=screenWidth;

        super.reshape(gl, x, y, width, height, screenWidth, screenHeight);
        setVoxel3DActorSizeParameters(x, y, width, height, screenWidth, screenHeight);
    }

    private void setVoxel3DActorSizeParameters(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        float[] parameters = computeOffsetParameters(x, y, width, height, screenWidth, screenHeight);
        setVoxel3DActorPostProjectionMatrix(getOffsetPostProjectionMatrix(parameters[0], parameters[1], parameters[2], parameters[3]));
        int[] xyBounds = getXYBounds(x, y, width, height);
        setVoxel3DxyBounds(xyBounds[0], xyBounds[1], xyBounds[2], xyBounds[3]);
    }

    @Override
    public void processEvent(AB2Event event) {
        super.processEvent(event);
        if (event instanceof AB2Main3DRendererSetRangeEvent) {
            AB2Main3DRendererSetRangeEvent rangeEvent = (AB2Main3DRendererSetRangeEvent) event;
            if (voxel3DActor!=null) {
                voxel3DActor.setIntensityRange(rangeEvent.getR0(), rangeEvent.getR1());
            }
        }
    }

    public void setRenderMode(Voxel3DActor.RenderMode renderMode) {
        if (voxel3DActor!=null) {
            voxel3DActor.setRenderMode(renderMode);
        }
    }

}
