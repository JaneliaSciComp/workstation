package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2ImageControlRequestCloseEvent;
import org.janelia.it.workstation.ab2.event.AB2ImageControlRequestOpenEvent;
import org.janelia.it.workstation.ab2.event.AB2RegionManagerResizeNeededEvent;
import org.janelia.it.workstation.ab2.renderer.AB2ImageControlPanelRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleBottomRegion extends AB2SideRegion {

    Logger logger = LoggerFactory.getLogger(AB2SampleBottomRegion.class);

    private AB2ImageControlPanelRenderer imageControlPanelRenderer;

    public AB2SampleBottomRegion(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        imageControlPanelRenderer=new AB2ImageControlPanelRenderer(x, y, width, height, screenWidth, screenHeight, this);

        renderers.add(imageControlPanelRenderer);
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        //logger.info("reshape() called with x="+x+" y="+y+" width="+width+" height="+height+" screenWidth="+screenWidth+" screenHeight="+screenHeight);
        GL4 gl4=(GL4)drawable.getGL();
        imageControlPanelRenderer.reshape(gl4, x, y, width, height, screenWidth, screenHeight);
    }

    public AB2ImageControlPanelRenderer getImageControlPanelRenderer() { return imageControlPanelRenderer; }

    @Override
    public void processEvent(AB2Event event) {
        //logger.info("processEvent() event="+event.getClass().getName());
        if (event instanceof AB2ImageControlRequestOpenEvent) {
            if (!isOpen()) {
                setOpen(true);
                imageControlPanelRenderer.setOpen(true);
                AB2Controller.getController().processEvent(new AB2RegionManagerResizeNeededEvent());
            }
        } else if (event instanceof AB2ImageControlRequestCloseEvent) {
            if (isOpen()) {
                setOpen(false);
                imageControlPanelRenderer.setOpen(false);
                AB2Controller.getController().processEvent(new AB2RegionManagerResizeNeededEvent());
            }
        }
        super.processEvent(event);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        if (isOpen()!=imageControlPanelRenderer.isOpen()) {
            imageControlPanelRenderer.setOpen(isOpen());
        }
        super.display(drawable);
    }

}
