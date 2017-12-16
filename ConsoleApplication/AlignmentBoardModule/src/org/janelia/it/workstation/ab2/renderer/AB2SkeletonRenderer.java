package org.janelia.it.workstation.ab2.renderer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.actor.BoundingBoxActor;
import org.janelia.it.workstation.ab2.actor.Camera3DFollowBoxActor;
import org.janelia.it.workstation.ab2.actor.Image2DActor;
import org.janelia.it.workstation.ab2.actor.Image3DActor;
import org.janelia.it.workstation.ab2.actor.LineSetActor;
import org.janelia.it.workstation.ab2.actor.PickSquareActor;
import org.janelia.it.workstation.ab2.actor.PointSetActor;
import org.janelia.it.workstation.ab2.actor.TextLabelActor;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.janelia.it.workstation.ab2.shader.AB2Basic2DShader;
import org.janelia.it.workstation.ab2.shader.AB2Basic3DShader;
import org.janelia.it.workstation.ab2.shader.AB2Image2DShader;
import org.janelia.it.workstation.ab2.shader.AB2PickShader;
import org.janelia.it.workstation.ab2.shader.AB2Text2DShader;
import org.janelia.it.workstation.ab2.test.AB2SimulatedVolumeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonRenderer extends AB2Renderer {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonRenderer.class);

    private SkeletonRenderer skeletonRenderer;
    private OverlayRenderer overlayRenderer;
    private AB2SimulatedVolumeGenerator volumeGenerator;
    private int actorCount=0;
    private boolean initialized=false;
    private Map<Long, Vector4> colorIdMap=new HashMap<>();
    private List<AB2NeuronSkeleton> skeletons;

    private class SkeletonRenderer extends AB2Renderer3D {

        private BoundingBoxActor boundingBoxActor;
        private Camera3DFollowBoxActor cameraFollowBoxActor;
        private Image3DActor image3DActor;

        private List<PointSetActor> pointSetActors=new ArrayList<>();
        private List<LineSetActor> lineSetActors=new ArrayList<>();

        GLShaderActionSequence basic3DShaderSequence=new GLShaderActionSequence("Basic3DSequence");

        public SkeletonRenderer() {
            super();
            addDrawShaderActionSequence(basic3DShaderSequence);
        }

        @Override
        public void init(GL4 gl) {
            addBoundingBox();
            addOriginPointActor();
            addSkeletonActors();
            addCameraFollowBoxActor();
            super.init(gl);
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

        private void addSkeletonActors() {
            if (skeletons==null) {
                return;
            }

            for (int i=0;i<skeletons.size();i++) {
                List<Vector3> skeletonPoints = skeletons.get(i).getSkeletonPointSet();
                List<Vector3> skeletonLines = skeletons.get(i).getSkeletonLineSet();
                Vector4 color=volumeGenerator.getColorByLabelIndex(i);

                PointSetActor pointSetActor = new PointSetActor(this, getNextActorIndex(), skeletonPoints);
                basic3DShaderSequence.getActorSequence().add(pointSetActor);
                colorIdMap.put(pointSetActor.getActorId(), color);

                LineSetActor lineSetActor = new LineSetActor(this, getNextActorIndex(), skeletonLines);
                basic3DShaderSequence.getActorSequence().add(lineSetActor);
                colorIdMap.put(lineSetActor.getActorId(), color);
            }
        }

        private void addBoundingBox() {
            // Bounding Box
            boundingBoxActor=new BoundingBoxActor(this, getNextActorIndex(), new Vector3(0f, 0f, 0f), new Vector3(1.0f, 1.0f, 1.0f));
            colorIdMap.put(boundingBoxActor.getActorId(), new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
            basic3DShaderSequence.getActorSequence().add(boundingBoxActor);
        }


    }

    private class OverlayRenderer extends AB2Renderer2D {

        private PickSquareActor pickSquareActor;
        private Image2DActor image2DActor;
        private TextLabelActor textLabelActor;

        GLShaderActionSequence basic2DShaderSequence=new GLShaderActionSequence("Basic2DSequence");
        GLShaderActionSequence image2DShaderSequence=new GLShaderActionSequence("Image2DSequence");
        GLShaderActionSequence text2DShaderSequence=new GLShaderActionSequence("Text2DSequence");
        GLShaderActionSequence pickShaderSequence=new GLShaderActionSequence("PickSequence");

        public OverlayRenderer() {
            super();
            addDrawShaderActionSequence(basic2DShaderSequence);
            addDrawShaderActionSequence(image2DShaderSequence);
            addDrawShaderActionSequence(text2DShaderSequence);
            addDrawShaderActionSequence(pickShaderSequence);
        }

        @Override
        public void init(GL4 gl) {
            addPickSquareActor();
            addImage2DActor();
            addTextLabelActor();
            super.init(gl);
        }

        private void addPickSquareActor() {
            // Pick Square
            pickSquareActor=new PickSquareActor(this, getNextActorIndex(), new Vector2(0.95f, 0.95f), new Vector2(1.0f, 1.0f),
                    new Vector4(1f, 0f, 0f, 1f), new Vector4(0f, 1f, 0f, 1f));
            colorIdMap.put(pickSquareActor.getActorId(), pickSquareActor.getColor0());
            basic2DShaderSequence.getActorSequence().add(pickSquareActor);
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
            image2DShaderSequence.getActorSequence().add(image2DActor);
            pickShaderSequence.getActorSequence().add(image2DActor);
        }

        private void addTextLabelActor() {
            // TextLabelActor
            Vector2 t0=new Vector2(0.1f, 0.2f);
            textLabelActor=new TextLabelActor(this, getNextActorIndex(), TextLabelActor.UBUNTU_FONT_STRING, t0,
                    new Vector4(1f, 1f, 1f, 1f), new Vector4(0.4f, 0.1f, 0.1f, 0.5f));
            text2DShaderSequence.getActorSequence().add(textLabelActor);
            pickShaderSequence.getActorSequence().add(textLabelActor);
        }

    }

    private int getNextActorIndex() { actorCount++; return actorCount; }

    public AB2SkeletonRenderer() {
        super();
        skeletonRenderer=new SkeletonRenderer();
        overlayRenderer=new OverlayRenderer();
    }

    @Override
    public void processEvent(AB2Event event) {
        skeletonRenderer.processEvent(event);
    }

    @Override
    public void init(GL4 gl) {
        skeletonRenderer.init(gl);
        overlayRenderer.init(gl);
        initialized=true;
    }

    public void setColorId(int styleId, Vector4 color) {
        colorIdMap.put(new Long(styleId), color);
    }

    public Vector4 getColorId(int styleId) {
        return colorIdMap.get(styleId);
    }

    public synchronized void setSkeletonsAndVolume(List<AB2NeuronSkeleton> skeletons, AB2SimulatedVolumeGenerator volumeGenerator) {
        this.skeletons=skeletons;
        this.volumeGenerator=volumeGenerator;
    }

    @Override
    public void dispose(GL4 gl) {
        skeletonRenderer.dispose(gl);
        overlayRenderer.dispose(gl);
    }

    @Override
    public void display(GL4 gl) {
        skeletonRenderer.display(gl);
        overlayRenderer.display(gl);
    }

    @Override
    public void reshape(GL4 gl, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        skeletonRenderer.reshape(gl, x, y, width, height, width, height);
        overlayRenderer.reshape(gl, x, y, width, height, width, height);
    }

}
