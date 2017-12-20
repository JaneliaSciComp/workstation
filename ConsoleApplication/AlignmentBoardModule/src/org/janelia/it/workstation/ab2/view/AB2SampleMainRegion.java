package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2SampleRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SampleMainRegion extends GLRegion {
    Logger logger = LoggerFactory.getLogger(AB2SampleMainRegion.class);


    private AB2SampleRenderer sampleRenderer=new AB2SampleRenderer();

    public AB2SampleMainRegion() {
        renderers.add(sampleRenderer);
    }

    public AB2SampleRenderer getSampleRenderer() {
        return sampleRenderer;
    }

    float scale;
    float xTranslate;
    float yTranslate;

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        // todo: fix this to use proper values for sampleRenderer.reshape()
        sampleRenderer.reshape(gl, x, y, screenWidth, screenHeight, screenWidth, screenHeight);
        float[] parameters=computeOffsetParameters(x, y, width, height, screenWidth, screenHeight);
        sampleRenderer.setVoxel3DActorPostProjectionMatrix(getOffsetPostProjectionMatrix(parameters[0], parameters[1], parameters[2]));
        int[] xyBounds=getXYBounds(x, y, width, height);
        sampleRenderer.setVoxel3DxyBounds(xyBounds[0], xyBounds[1], xyBounds[2], xyBounds[3]);
    }

    @Override
    public void processEvent(AB2Event event) {
        sampleRenderer.processEvent(event);
    }


}
