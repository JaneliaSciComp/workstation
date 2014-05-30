package org.janelia.it.workstation.gui.opengl.stereo3d;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.gui.opengl.GLActorContext;
import org.janelia.it.workstation.gui.opengl.GLSceneComposer;

public class AnaglyphRedCyanStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable); // Just one viewport for anaglyph
        GL gl = glDrawable.getGL();

        // Red left
        setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glColorMask(true, false, false, true);
        composer.displayScene(actorContext);

        // Cyan right
        setRightEyeView(actorContext, composer.getCameraScreenGeometry());
        gl.glColorMask(false, true, true, true);
        composer.displayScene(actorContext);

        // Restore full color
        gl.glColorMask(true, true, true, true);
    }

}
