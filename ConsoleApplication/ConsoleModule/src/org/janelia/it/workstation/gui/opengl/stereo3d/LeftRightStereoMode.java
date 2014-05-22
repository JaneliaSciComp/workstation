package org.janelia.it.workstation.gui.opengl.stereo3d;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.gui.opengl.GLActorContext;

public class LeftRightStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            org.janelia.it.workstation.gui.opengl.GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        GL gl = glDrawable.getGL();
        gl.glEnable(GL.GL_SCISSOR_TEST);

        // Left
        setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glViewport(0, 0, viewportWidth, viewportHeight);
        gl.glScissor(0, 0, viewportWidth, viewportHeight);
        composer.displayScene(actorContext);

        // Right
        setRightEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glViewport(viewportWidth, 0, viewportWidth, viewportHeight);
        gl.glScissor(viewportWidth, 0, viewportWidth, viewportHeight);
        composer.displayScene(actorContext);
        
        // Restore full display
        gl.glScissor(0, 0, viewportWidth*2, viewportHeight);
    }

    @Override
    public void reshape(int width, int height) 
    {
        // Use only half width at one time
        super.reshape(width/2, height);
    }

}
