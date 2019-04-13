
package org.janelia.scenewindow.stereo;

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.LateralOffsetCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class HardwareRenderer implements StereoRenderer {
    // TODO - factor out stereo rig
    private PerspectiveCamera monoCamera = null;
    private AbstractCamera leftCamera = null;
    private AbstractCamera rightCamera = null;
    private final float ipdPixels = 120.0f; // TODO - rational choice of stereo parameters

    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) {
        GL3 gl = new DebugGL3(glDrawable.getGL().getGL3());
        
        if (! canDisplay(glDrawable)) {
            renderer.renderScene(gl, renderer.getCamera());
            return;
        }
        
        if (renderer.getCamera() != monoCamera) {
            monoCamera = (PerspectiveCamera) renderer.getCamera();
            leftCamera  = new LateralOffsetCamera(monoCamera, +0.5f * ipdPixels);
            rightCamera = new LateralOffsetCamera(monoCamera, -0.5f * ipdPixels);
        }
        
        // Left eye in one color...
        gl.glDrawBuffer(GL2.GL_BACK_LEFT);
        if (swapEyes)
            renderer.renderScene(gl, rightCamera);
        else
            renderer.renderScene(gl, leftCamera);

        // ...Right eye, complementary color
        gl.glDrawBuffer(GL2.GL_BACK_RIGHT);
        // Clear depth buffer, but not color buffer
        gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
        if (swapEyes)
            renderer.renderScene(gl, leftCamera);
        else
            renderer.renderScene(gl, rightCamera);
        
        // Restore default color mask
        gl.glDrawBuffer(GL2.GL_BACK);
    }
    
    public static boolean canDisplay(GLDrawable glDrawable) {
            GLCapabilitiesImmutable glCaps = glDrawable.getChosenGLCapabilities();
            return glCaps.getStereo();
    }
    
}
