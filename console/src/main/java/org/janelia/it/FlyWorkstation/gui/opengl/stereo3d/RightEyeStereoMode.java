package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActorContext;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;

public class RightEyeStereoMode extends BasicStereoMode
{
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer) 
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable);
        setRightEyeView(actorContext, composer.getCameraScreenGeometry());
        composer.displayScene(actorContext);
    }

}
