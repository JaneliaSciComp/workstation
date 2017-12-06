package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2SampleRenderer;

public class AB2SampleMainRegion extends GLRegion {

    private AB2SampleRenderer sampleRenderer=new AB2SampleRenderer();

    public AB2SampleMainRegion() {
        renderers.add(sampleRenderer);
    }

    public AB2SampleRenderer getSampleRenderer() {
        return sampleRenderer;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        final GL4 gl=drawable.getGL().getGL4();
        sampleRenderer.reshape(gl, x, y, width, height, screenWidth, screenHeight);
    }
}
