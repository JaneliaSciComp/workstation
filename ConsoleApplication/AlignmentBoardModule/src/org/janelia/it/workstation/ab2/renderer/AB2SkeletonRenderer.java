package org.janelia.it.workstation.ab2.renderer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.media.opengl.GL4;
import javax.swing.ImageIcon;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.jacs.model.ontology.types.Text;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.Camera3DFollowBoxActor;
import org.janelia.it.workstation.ab2.actor.Image2DActor;
import org.janelia.it.workstation.ab2.actor.Image3DActor;
import org.janelia.it.workstation.ab2.actor.LineSetActor;
import org.janelia.it.workstation.ab2.actor.PickSquareActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLActorUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderUpdateCallback;
import org.janelia.it.workstation.ab2.model.AB2Image3D_RGBA8UI;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.janelia.it.workstation.ab2.shader.AB2ActorPickShader;
import org.janelia.it.workstation.ab2.shader.AB2ActorShader;
import org.janelia.it.workstation.ab2.test.AB2SimulatedVolumeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonRenderer extends AB23DRenderer {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonRenderer.class);

    private Matrix4 modelMatrix;
    private BoundingBoxActor boundingBoxActor;
    private Camera3DFollowBoxActor cameraFollowBoxActor;

    private List<AB2NeuronSkeleton> skeletons;
    private List<PointSetActor> pointSetActors=new ArrayList<>();
    private List<LineSetActor> lineSetActors=new ArrayList<>();

    private PickSquareActor pickSquareActor;
    private Image2DActor image2DActor;
    private Image3DActor image3DActor;
    private TextLabelActor textLabelActor;

    AB2SimulatedVolumeGenerator volumeGenerator;

    GLShaderActionSequence drawShaderSequence;
    GLShaderActionSequence pickShaderSequence;

    //static final int BOUNDING_BOX_ID=1;

    int actorCount=0;

    private int getNextActorIndex() { actorCount++; return actorCount; }

    public AB2SkeletonRenderer() {
        super();

        drawShaderSequence=new GLShaderActionSequence("DrawSequence");
        pickShaderSequence=new GLShaderActionSequence("PickSequence");

        drawShaderSequence.setShader(new AB2ActorShader());
        pickShaderSequence.setShader(new AB2ActorPickShader());

        addDrawShaderActionSequence(drawShaderSequence);
        addPickShaderActionSequence(pickShaderSequence);
    }

    @Override
    public void init(GL4 gl) {

        addBoundingBox();
        addOriginPointActor();
//        addSkeletonActors();
        addImage3DActor();

        addPickSquareActor();
        addImage2DActor();
        addTextLabelActor();

        addCameraFollowBoxActor();

        super.init(gl);
        initialized=true;
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

    private void addSkeletonActors() {
        if (skeletons==null) {
            return;
        }

        for (int i=0;i<skeletons.size();i++) {
            List<Vector3> skeletonPoints = skeletons.get(i).getSkeletonPointSet();
            List<Vector3> skeletonLines = skeletons.get(i).getSkeletonLineSet();
            Vector4 color=volumeGenerator.getColorByLabelIndex(i);

            PointSetActor pointSetActor = new PointSetActor(this, getNextActorIndex(), skeletonPoints);
            drawShaderSequence.getActorSequence().add(pointSetActor);
            colorIdMap.put(pointSetActor.getActorId(), color);

            LineSetActor lineSetActor = new LineSetActor(this, getNextActorIndex(), skeletonLines);
            drawShaderSequence.getActorSequence().add(lineSetActor);
            colorIdMap.put(lineSetActor.getActorId(), color);
        }
    }

    private void addImage3DActor() {
        AB2Image3D_RGBA8UI rawImage=volumeGenerator.getRawImage();
        Vector3 v0=new Vector3(0f, 0f, 0f);
        Vector3 v1=new Vector3(1f, 1f, 1f);
        image3DActor=new Image3DActor(this, getNextActorIndex(), v0, v1, rawImage.getXDim(), rawImage.getYDim(), rawImage.getZDim(), rawImage.getData());
        drawShaderSequence.getActorSequence().add(image3DActor);
    }

    private void addBoundingBox() {
        // Bounding Box
        boundingBoxActor=new BoundingBoxActor(this, getNextActorIndex(), new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
        colorIdMap.put(boundingBoxActor.getActorId(), new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
        drawShaderSequence.getActorSequence().add(boundingBoxActor);
    }

    private void addPickSquareActor() {
        // Pick Square
        pickSquareActor=new PickSquareActor(this, getNextActorIndex(), new Vector2(0.95f, 0.95f), new Vector2(1.0f, 1.0f),
                new Vector4(1f, 0f, 0f, 1f), new Vector4(0f, 1f, 0f, 1f));
        colorIdMap.put(pickSquareActor.getActorId(), pickSquareActor.getColor0());
        drawShaderSequence.getActorSequence().add(pickSquareActor);
        pickShaderSequence.getActorSequence().add(pickSquareActor);
    }

    private void addImage2DActor() {
        // Image2DActor
        BufferedImage bufferedImage=GLAbstractActor.getImageByFilename("UbuntuFont.png");
        int screenWidth=viewport.getWidthPixels();
        int screenHeight=viewport.getHeightPixels();

        if (screenWidth==0) {
            screenWidth=AB2Controller.getController().getGljPanel().getSurfaceWidth();
        }
        if (screenHeight==0) {
            screenHeight=AB2Controller.getController().getGljPanel().getSurfaceHeight();
        }
        float imageNormalHeight=(float)((bufferedImage.getHeight()*1.0)/screenHeight);
        float imageAspectRatio=(float)((bufferedImage.getWidth()*1.0)/(bufferedImage.getHeight()*1.0));
        float imageNormalWidth=imageNormalHeight*imageAspectRatio;
        logger.info("imageNormalWidth="+imageNormalWidth);
        logger.info("imageNormalHeight="+imageNormalHeight);
        Vector2 v0=new Vector2(0.1f, 0.6f);
        Vector2 v1=new Vector2(v0.get(0)+imageNormalWidth, v0.get(1)+imageNormalHeight);
        image2DActor=new Image2DActor(this, getNextActorIndex(), v0, v1, bufferedImage, 1.0f);
        colorIdMap.put(image2DActor.getActorId(), new Vector4(0f, 0f, 1f, 1f));
        drawShaderSequence.getActorSequence().add(image2DActor);
        pickShaderSequence.getActorSequence().add(image2DActor);
    }

    private void addTextLabelActor() {
        // TextLabelActor
        Vector2 t0=new Vector2(0.1f, 0.2f);
        textLabelActor=new TextLabelActor(this, getNextActorIndex(), TextLabelActor.UBUNTU_FONT_STRING, t0,
                new Vector4(1f, 1f, 1f, 1f), new Vector4(0.4f, 0.1f, 0.1f, 0.5f));
        drawShaderSequence.getActorSequence().add(textLabelActor);
        pickShaderSequence.getActorSequence().add(textLabelActor);
    }

    public void setColorId(int styleId, Vector4 color) {
        colorIdMap.put(styleId, color);
    }

    public Vector4 getColorId(int styleId) {
        return colorIdMap.get(styleId);
    }


    public synchronized void setSkeletons(List<AB2NeuronSkeleton> skeletons) {
        this.skeletons=skeletons;
        volumeGenerator=new AB2SimulatedVolumeGenerator(512, 512, 512); // 800 X 3, close to limit for byte array length

        for (int i=0;i<skeletons.size();i++) {
            logger.info("Skeleton "+i);
            volumeGenerator.addSkeleton(skeletons.get(i));
        }

        logger.info("Added all skeletons to Simulated Volume");

        logger.info("Starting dilation");

        volumeGenerator.performDilation(2.0f, 0.0f);

        logger.info("Dilation finished");

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
