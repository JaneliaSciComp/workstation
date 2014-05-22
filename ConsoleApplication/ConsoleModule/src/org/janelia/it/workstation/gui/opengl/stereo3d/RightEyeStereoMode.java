package org.janelia.it.workstation.gui.opengl.stereo3d;

import javax.media.opengl.GLAutoDrawable;

public class RightEyeStereoMode extends BasicStereoMode
{
    @Override
    public void display(org.janelia.it.workstation.gui.opengl.GLActorContext actorContext,
            org.janelia.it.workstation.gui.opengl.GLSceneComposer composer)
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable);
        setRightEyeView(actorContext, composer.getCameraScreenGeometry());
        composer.displayScene(actorContext);
    }

}
