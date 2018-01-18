package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2MenuPanelRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2SamplePanelRenderer;

public class AB2SampleLeftRegion extends AB2SideRegion {

    private AB2SamplePanelRenderer samplePanelRenderer;

    public AB2SampleLeftRegion(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        samplePanelRenderer=new AB2SamplePanelRenderer(x, y, width, height, screenWidth, screenHeight, this);

        renderers.add(samplePanelRenderer);
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        GL4 gl4=(GL4)drawable.getGL();
        samplePanelRenderer.reshape(gl4, x, y, width, height, screenWidth, screenHeight);
    }
}
