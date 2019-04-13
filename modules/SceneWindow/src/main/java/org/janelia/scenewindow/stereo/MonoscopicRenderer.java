
package org.janelia.scenewindow.stereo;

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class MonoscopicRenderer implements StereoRenderer 
{
    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) {
        GL3 gl = new DebugGL3(glDrawable.getGL().getGL3());
        renderer.renderScene(gl, renderer.getCamera());
    }
}
