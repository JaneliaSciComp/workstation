package org.janelia.it.workstation.ab2.controller;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.janelia.it.workstation.ab2.event.AB2DomainObjectUpdateEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.model.AB2SkeletonDomainObject;
import org.janelia.it.workstation.ab2.renderer.AB2SkeletonRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonMode extends AB2View3DMode {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonMode.class);


    public AB2SkeletonMode(AB2Controller controller, AB2SkeletonRenderer renderer) {
        super(controller, renderer);
        logger.info("AB2SkeletonMode() constructor finished");
    }

    @Override
    public void processEvent(AB2Event event) {
        //logger.info("processEvent()");
        super.processEvent(event);
        if  (event instanceof AB2DomainObjectUpdateEvent) {
            ((AB2SkeletonRenderer)renderer).setSkeletons(((AB2SkeletonDomainObject)controller.getDomainObject()).getSkeletons());
            controller.repaint();
        } else if (event instanceof AB2MouseClickedEvent) {
            MouseEvent mouseEvent=((AB2MouseClickedEvent) event).getMouseEvent();
            int x = mouseEvent.getX();
            int y = mouseEvent.getY(); // y is inverted - 0 is at the top
            // Check the pick buffer
            AB2SkeletonRenderer skeletonRenderer = (AB2SkeletonRenderer)renderer;
            //int pickId=skeletonRenderer.getPickBufferAtXY(x, y, true);
            //logger.info("MouseClick PickId="+pickId);
        }
    }

}
