
package org.janelia.scenewindow.stereo;

import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.camera.BasicFrustumShift;
import org.janelia.geometry3d.camera.ConstFrustumShift;
import org.janelia.geometry3d.camera.ShiftableCamera;
import org.janelia.scenewindow.SceneRenderer;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class LeftEyeRenderer implements StereoRenderer 
{
    private float ipdPixels = 60.0f; // TODO - rational choice of stereo parameters

    public LeftEyeRenderer(float ipdPixels) {
        this.ipdPixels = ipdPixels;
    }
    
    @Override
    public void renderScene(GLAutoDrawable glDrawable, SceneRenderer renderer, boolean swapEyes) 
    {
        float shift = -0.5f * ipdPixels;
        if (swapEyes)
            shift = -shift;
        ConstFrustumShift frustumShift = new BasicFrustumShift(shift, 0);
        
        AbstractCamera camera = renderer.getCamera();
        GL3 gl = new DebugGL3(glDrawable.getGL().getGL3());
        try {
            if (camera instanceof ShiftableCamera)
                ((ShiftableCamera)camera).pushFrustumShift(frustumShift);
            renderer.renderScene(gl, camera);
        }
        finally {
            if (camera instanceof ShiftableCamera)
                ((ShiftableCamera)camera).popFrustumShift();            
        }
    }
}
