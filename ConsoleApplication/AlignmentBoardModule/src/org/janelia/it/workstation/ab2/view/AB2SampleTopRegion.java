package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.renderer.AB2MenuPanelRenderer;

public class AB2SampleTopRegion extends AB2SideRegion {
    public static int OPEN_HEIGHT=100;
    public static int CLOSED_HEIGHT=30;

    private AB2MenuPanelRenderer menuPanelRenderer;

    public AB2SampleTopRegion() {
        menuPanelRenderer=new AB2MenuPanelRenderer();
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        GL4 gl4=(GL4)drawable.getGL();
        menuPanelRenderer.reshape(gl4, x, y, width, height, screenWidth, screenHeight);
    }
}
