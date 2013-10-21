package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;

import org.janelia.it.FlyWorkstation.gui.opengl.GLActorContext;
import org.janelia.it.FlyWorkstation.gui.opengl.GLError;
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
            GLError.checkGlError(gl, "hardware stereo 44");
            GL2GL3 gl2gl3 = gl.getGL2GL3();
            
            gl.glClearColor(0,0,0,0);
            
            gl2gl3.glDrawBuffer(GL2GL3.GL_BACK);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT); // To avoid ghost image on Mac
            
            // Right
            setRightEyeView(actorContext, composer.getCameraScreenGeometry());
            gl2gl3.glDrawBuffer(GL2.GL_BACK_RIGHT);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            composer.displayScene(actorContext);
            
            GLError.checkGlError(gl, "hardware stereo 59");
            // Left
            setLeftEyeView(actorContext, composer.getCameraScreenGeometry());
            gl2gl3.glDrawBuffer(GL2.GL_BACK_LEFT);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            composer.displayScene(actorContext);
    
            // Restore default double buffer mode
            GLError.checkGlError(gl, "hardware stereo 69");
            gl2gl3.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
            GLError.checkGlError(gl, "hardware stereo 71");
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
