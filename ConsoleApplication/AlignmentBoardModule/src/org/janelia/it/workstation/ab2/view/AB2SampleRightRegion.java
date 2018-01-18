package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2NeuronPanelRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2SamplePanelRenderer;

public class AB2SampleRightRegion extends AB2SideRegion {

    private AB2NeuronPanelRenderer neuronPanelRenderer;

    public AB2SampleRightRegion(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        neuronPanelRenderer=new AB2NeuronPanelRenderer(x, y, width, height, screenWidth, screenHeight, this);

        renderers.add(neuronPanelRenderer);
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        GL4 gl4=(GL4)drawable.getGL();
        neuronPanelRenderer.reshape(gl4, x, y, width, height, screenWidth, screenHeight);
    }
}

