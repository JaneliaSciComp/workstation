package org.janelia.it.workstation.gui.opengl.stereo3d;

import org.janelia.it.workstation.gui.opengl.GLActorContext;
import org.janelia.it.workstation.gui.opengl.GLSceneComposer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

public class AnaglyphGreenMagentaStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable); // Just one viewport for anaglyph
        GL gl = glDrawable.getGL();

        // Green left
        setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glColorMask(false, true, false, true);
        composer.displayScene(actorContext);

        // Magenta right
        setRightEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glColorMask(true, false, true, true);
        composer.displayScene(actorContext);

        // Restore full color
        gl.glColorMask(true, true, true, true);
    }

}
