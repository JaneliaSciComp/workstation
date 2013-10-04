package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActorContext;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;

public class LeftRightStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer) 
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
    public void reshape(GLAutoDrawable glDrawable, int x, int y, 
            int width, int height) 
    {
        // Use only half width at one time
        super.reshape(glDrawable, x, y, width/2, height);
    }

}
