package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2MouseClickedEvent;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2ImageControlPanelRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2MenuPanelRenderer;

public class AB2SampleBottomRegion extends AB2SideRegion {

    private AB2ImageControlPanelRenderer imageControlPanelRenderer;

    public AB2SampleBottomRegion(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        imageControlPanelRenderer=new AB2ImageControlPanelRenderer(x, y, width, height, screenWidth, screenHeight);

        renderers.add(imageControlPanelRenderer);
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        GL4 gl4=(GL4)drawable.getGL();
        imageControlPanelRenderer.reshape(gl4, x, y, width, height, screenWidth, screenHeight);
    }

    @Override
    public void processEvent(AB2Event event) {
        if (event instanceof AB2MouseClickedEvent) {
            if (isOpen()) {
                setOpen(false);
            } else {
                setOpen(true);
            }
        }
        super.processEvent(event);
    }
}
