
package org.janelia.scenewindow.stereo;

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.LateralOffsetCamera;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class AnaglyphRenderer implements StereoRenderer {
    final boolean leftColors[] = {true, true, true};
    
    // TODO - factor out stereo rig
    private PerspectiveCamera monoCamera = null;
    private AbstractCamera leftCamera = null;
    private AbstractCamera rightCamera = null;
    private final float ipdPixels = 120.0f; // TODO - rational choice of stereo parameters

    public AnaglyphRenderer(boolean leftRed, boolean leftGreen, boolean leftBlue) { // TODO - boolean trap...
        leftColors[0] = leftRed;
        leftColors[1] = leftGreen;
        leftColors[2] = leftBlue;
    }
    
    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) {
        GL3 gl = new DebugGL3(glDrawable.getGL().getGL3());

        if (false) {
            // FOR TESTING ONLY
            gl.glColorMask(true, false, false, true);
            renderer.renderScene(gl, renderer.getCamera());
            gl.glColorMask(false, true, true, true);
            renderer.renderScene(gl, renderer.getCamera());
            gl.glColorMask(true, true, true, true);
        }
        else {
            if (renderer.getCamera() != monoCamera) {
                monoCamera = (PerspectiveCamera) renderer.getCamera();
                leftCamera  = new LateralOffsetCamera(monoCamera, -0.5f * ipdPixels);
                rightCamera = new LateralOffsetCamera(monoCamera, +0.5f * ipdPixels);
            }

            // Notify observers that the camera has changed...
            monoCamera.getVantage().setChanged();
            monoCamera.getVantage().notifyObservers();

            // GL3 gl = glDrawable.getGL().getGL3();
            // Left eye in one color...
            gl.glColorMask(leftColors[0], leftColors[1], leftColors[2], true);
            if (swapEyes)
                renderer.renderScene(gl, rightCamera);
            else
                renderer.renderScene(gl, leftCamera);

            // ...Right eye, complementary color
            gl.glColorMask( !leftColors[0], !leftColors[1], !leftColors[2], true);
            // Clear depth buffer, but not color buffer
            gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
            
            // Notify observers that the camera has changed...
            monoCamera.getVantage().setChanged();
            monoCamera.getVantage().notifyObservers();
            
            if (swapEyes)
                renderer.renderScene(gl, leftCamera);
            else
                renderer.renderScene(gl, rightCamera);

            // Restore default color mask
            gl.glColorMask(true, true, true, true);
        }
    }    
}
