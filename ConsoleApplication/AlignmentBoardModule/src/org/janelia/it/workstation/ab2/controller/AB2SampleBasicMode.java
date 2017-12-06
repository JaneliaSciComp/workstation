package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.actor.Image2DActor;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2Image2DClickEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.ab2.event.AB2SampleAddedEvent;
import org.janelia.it.workstation.ab2.loader.AB2Sample3DImageLoader;
import org.janelia.it.workstation.ab2.renderer.AB2Renderer;
import org.janelia.it.workstation.ab2.renderer.AB2SampleRenderer;
import org.janelia.it.workstation.ab2.view.AB2SampleRegionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleBasicMode extends AB2View3DMode {

    Logger logger= LoggerFactory.getLogger(AB2SampleBasicMode.class);

    private AB2SampleRegionManager sampleRegionManager=new AB2SampleRegionManager();

    private AB2SampleRenderer sampleRenderer;


    public AB2SampleBasicMode(AB2Controller controller) {
        super(controller);
        sampleRenderer=sampleRegionManager.getMainRegion().getSampleRenderer();
        logger.info("AB2SampleBasicMode() constructor finished");
    }

    @Override
    public AB2Renderer getRendererAtPosition(Point point) {
        return sampleRenderer;
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.init(glAutoDrawable);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.dispose(glAutoDrawable);
        System.gc();
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        sampleRegionManager.display(glAutoDrawable);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {
        sampleRegionManager.reshape(glAutoDrawable, i, i1, i2, i3);
    }

    @Override
    public void processEvent(AB2Event event) {
        super.processEvent(event);
        if (event instanceof AB2SampleAddedEvent) {
            AB2SampleAddedEvent sampleAddedEvent=(AB2SampleAddedEvent)event;
            sampleRenderer.clearActors();
            AB2Sample3DImageLoader sample3DImageLoader=new AB2Sample3DImageLoader(sampleAddedEvent.getSample());
            sample3DImageLoader.execute();
        } else if  (event instanceof AB2Sample3DImageLoadedEvent) {
            AB2Sample3DImageLoadedEvent sample3DImageLoadedEvent=(AB2Sample3DImageLoadedEvent)event;
            sampleRenderer.addSample3DImage(sample3DImageLoadedEvent.getData());
            sample3DImageLoadedEvent.clearData();
            controller.repaint();
        } else if (event instanceof AB2MouseClickedEvent) {
            MouseEvent mouseEvent=((AB2MouseClickedEvent) event).getMouseEvent();
            int x = mouseEvent.getX();
            int y = mouseEvent.getY(); // y is inverted - 0 is at the top
            sampleRenderer.addMouseClickEvent(x, y);
            controller.repaint();
        } else if (event instanceof AB2Image2DClickEvent) {
            Image2DActor image2DActor=((AB2Image2DClickEvent)event).getImage2DActor();
            logger.info("Handling AB2Image2DClickEvent - actorId="+image2DActor.getActorId()+" pickIndex="+image2DActor.getPickIndex());
        }
    }


}
