
package org.janelia.scenewindow.stereo;

import javax.media.opengl.GLAutoDrawable;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public interface StereoRenderer {
    void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes);
}
