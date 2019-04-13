package org.janelia.workstation.gui.opengl.stereo3d;

import javax.media.opengl.GLAutoDrawable;
import org.janelia.workstation.gui.opengl.GLActorContext;
import org.janelia.workstation.gui.opengl.GLSceneComposer;

public class LeftEyeStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable);
        setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
        composer.displayScene(actorContext);
    }

}
