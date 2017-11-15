package org.janelia.it.workstation.ab2.controller;

import java.awt.event.MouseEvent;

import org.janelia.it.workstation.ab2.actor.Image2DActor;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2Image2DClickEvent;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.event.AB2Sample3DImageLoadedEvent;
import org.janelia.it.workstation.ab2.event.AB2SampleAddedEvent;
import org.janelia.it.workstation.ab2.loader.AB2Sample3DImageLoader;
import org.janelia.it.workstation.ab2.renderer.AB2SampleRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleBasicMode extends AB2View3DMode {

    Logger logger= LoggerFactory.getLogger(AB2SampleBasicMode.class);


    public AB2SampleBasicMode(AB2Controller controller, AB2SampleRenderer renderer) {
        super(controller, renderer);
        logger.info("AB2SampleBasicMode() constructor finished");
    }

    @Override
    public void processEvent(AB2Event event) {
        AB2SampleRenderer sampleRenderer=(AB2SampleRenderer)renderer;
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
            renderer.addMouseClickEvent(x, y);
            controller.repaint();
        } else if (event instanceof AB2Image2DClickEvent) {
            Image2DActor image2DActor=((AB2Image2DClickEvent)event).getImage2DActor();
            logger.info("Handling AB2Image2DClickEvent - actorId="+image2DActor.getActorId()+" pickIndex="+image2DActor.getPickIndex());
        }
    }


}
