package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.renderer.AB2MenuPanelRenderer;

public class AB2SampleTopRegion extends AB2SideRegion {

    private AB2MenuPanelRenderer menuPanelRenderer;

    public AB2SampleTopRegion(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        menuPanelRenderer=new AB2MenuPanelRenderer(x, y, width, height, screenWidth, screenHeight, this);

        renderers.add(menuPanelRenderer);
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        GL4 gl4=(GL4)drawable.getGL();
        menuPanelRenderer.reshape(gl4, x, y, width, height, screenWidth, screenHeight);
    }
}
