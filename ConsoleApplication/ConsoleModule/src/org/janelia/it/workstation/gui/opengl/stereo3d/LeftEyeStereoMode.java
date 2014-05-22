package org.janelia.it.workstation.gui.opengl.stereo3d;

import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.gui.opengl.GLActorContext;

public class LeftEyeStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            org.janelia.it.workstation.gui.opengl.GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable);
        setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
        composer.displayScene(actorContext);
    }

}
