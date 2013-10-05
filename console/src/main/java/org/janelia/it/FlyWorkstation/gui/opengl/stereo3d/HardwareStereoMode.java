package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;

import org.janelia.it.FlyWorkstation.gui.opengl.GLActorContext;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;

public class HardwareStereoMode extends BasicStereoMode 
{
    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer) 
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable);

        if (canDisplay(glDrawable)) {
            GL gl = glDrawable.getGL();
            GL2GL3 gl2gl3 = gl.getGL2GL3();
            
            // Left
            setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
            gl2gl3.glDrawBuffer(GL2.GL_BACK_LEFT);
            composer.displayScene(actorContext);
    
            // Right
            setRightEyeView(actorContext, composer.getCameraScreenGeometry());
            gl2gl3.glDrawBuffer(GL2.GL_BACK_RIGHT);
            composer.displayScene(actorContext);
            
            // Restore default double buffer mode
            gl2gl3.glDrawBuffer(GL2GL3.GL_BACK);
        }
        else {
            setMonoscopicView(actorContext, composer.getCameraScreenGeometry());
            composer.displayScene(actorContext);
        }
    }

    public boolean canDisplay(GLDrawable glDrawable) {
		GLCapabilitiesImmutable glCaps = glDrawable.getChosenGLCapabilities();
		return glCaps.getStereo();
    }
}
