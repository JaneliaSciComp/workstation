package org.janelia.it.workstation.ab2.view;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.gl.GLRegion;
import org.janelia.it.workstation.ab2.renderer.AB2Main3DRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.janelia.it.workstation.ab2.renderer.AB2RendererD.computeOffsetParameters;
import static org.janelia.it.workstation.ab2.renderer.AB2RendererD.getOffsetPostProjectionMatrix;

public class AB2SampleMainRegion extends GLRegion {
    Logger logger = LoggerFactory.getLogger(AB2SampleMainRegion.class);


    private AB2Main3DRenderer main3DRenderer;

    public AB2SampleMainRegion(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.screenWidth=screenWidth;
        this.screenHeight=screenHeight;

        setHoverable(true);

        main3DRenderer=new AB2Main3DRenderer(x, y, width, height, screenWidth, screenHeight, this);

        renderers.add(main3DRenderer);
    }

    public AB2Main3DRenderer getSampleRenderer() {
        return main3DRenderer;
    }

    @Override
    protected void reshape(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        main3DRenderer.reshape(gl, x, y, width, height, screenWidth, screenHeight);
    }

    @Override
    public void processEvent(AB2Event event) {
        //logger.info("processEvent() received event type="+event.getClass().getName());
        main3DRenderer.processEvent(event);
    }


}
