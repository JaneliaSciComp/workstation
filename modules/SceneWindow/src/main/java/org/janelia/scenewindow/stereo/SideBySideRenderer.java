
package org.janelia.scenewindow.stereo;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.LateralOffsetCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Viewport;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class SideBySideRenderer implements StereoRenderer {
    private PerspectiveCamera monoCamera = null;
    private AbstractCamera leftCamera = null;
    private AbstractCamera rightCamera = null;
    private final float ipdPixels = 120.0f; // TODO - rational choice of stereo parameters

    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) {
        if (renderer.getCamera() != monoCamera) {
            monoCamera = (PerspectiveCamera) renderer.getCamera();
            leftCamera  = new LateralOffsetCamera(monoCamera, 
                +0.5f * ipdPixels,
                0, 0, 0.5f, 1.0f);
            rightCamera = new LateralOffsetCamera(monoCamera, 
                -0.5f * ipdPixels,
                0.5f, 0, 0.5f, 1.0f);
        }
        
        GL3 gl = glDrawable.getGL().getGL3();
        gl.glEnable(GL.GL_SCISSOR_TEST);
        
        // Left eye in left half
        Viewport v = leftCamera.getViewport();
        gl.glScissor(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        gl.glViewport(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        if (swapEyes)
            renderer.renderScene(gl, rightCamera);
        else
            renderer.renderScene(gl, leftCamera);            

        // Right eye in right half
        v = rightCamera.getViewport();
        gl.glScissor(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        gl.glViewport(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        if (swapEyes)
            renderer.renderScene(gl, leftCamera);
        else
            renderer.renderScene(gl, rightCamera);            

        // Restore default state
        v = monoCamera.getViewport();
        gl.glViewport(v.getOriginXPixels(), v.getOriginYPixels(),
                v.getWidthPixels(), v.getHeightPixels());
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }
    
}
