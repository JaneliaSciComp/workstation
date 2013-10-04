package org.janelia.it.FlyWorkstation.gui.opengl.stereo3d;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.CameraScreenGeometry;
import org.janelia.it.FlyWorkstation.gui.opengl.GL2Adapter;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActorContext;
import org.janelia.it.FlyWorkstation.gui.opengl.GL2Adapter.MatrixMode;
import org.janelia.it.FlyWorkstation.gui.opengl.GLSceneComposer;

public class BasicStereoMode implements StereoMode {
    protected int viewportWidth = 1;
    protected int viewportHeight = 1;
    private boolean eyesSwapped = false;

    @Override
    public void display(GLActorContext actorContext,
            GLSceneComposer composer) 
    {
        GLAutoDrawable glDrawable = actorContext.getGLAutoDrawable();
        updateViewport(glDrawable);
        updateProjectionMatrix(actorContext, composer.getCameraScreenGeometry(), 0);
        composer.displayScene(actorContext);
    }

    public boolean isEyesSwapped() {
        return eyesSwapped;
    }

    public void setEyesSwapped(boolean eyesSwapped) {
        this.eyesSwapped = eyesSwapped;
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, 
            int width, int height) 
    {
        int w = Math.max(width, 1);
        int h = Math.max(height, 1);
        if ((w == viewportWidth) && (h == viewportHeight))
            return; // no change
        viewportWidth = w;
        viewportHeight = h;
        updateViewport(glDrawable);
    }

    protected void setLeftEyeView(GLActorContext actorContext,
            CameraScreenGeometry cameraScreenGeometry) 
    {
        updateProjectionMatrix(actorContext, cameraScreenGeometry, -1.0);
    }

    protected void setMonoscopicView(GLActorContext actorContext,
            CameraScreenGeometry cameraScreenGeometry) 
    {
        updateProjectionMatrix(actorContext, cameraScreenGeometry, 0);
    }

    protected void setRightEyeView(GLActorContext actorContext,
            CameraScreenGeometry cameraScreenGeometry) 
    {
        updateProjectionMatrix(actorContext, cameraScreenGeometry, 1.0);
    }

    protected void updateViewport(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        gl.glViewport(0, 0, viewportWidth, viewportHeight);
    }

    /**
     * 
     * @param gl
     * @param eyeShift -1 for left eye, +1 for right eye, 0 for mono
     */
    protected void updateProjectionMatrix(GLActorContext actorContext,
            CameraScreenGeometry cameraScreenGeometry, double eyeShift) 
    {
        if (isEyesSwapped())
            eyeShift = -eyeShift;
        int w = viewportWidth;
        int h = viewportHeight;
        double aspect = w/(double)h;
        // Convert between 3 coordinate systems:
        //  1) real world/user/lab in units of centimeters (cm)
        //  2) monitor screen in units of pixels (px)
        //  3) scene world in scene units (scene)
        // Distance from user/camera to screen/focal point
        double camDistCm = cameraScreenGeometry.getScreenEyeDistanceCm();
        double camDistPx = camDistCm
                * cameraScreenGeometry.getScreenPixelsPerCm();
        Camera3d camera = cameraScreenGeometry.getCamera();
        double camDistScene = camDistPx / camera.getPixelsPerSceneUnit();
        double tanx = camDistPx;
        double tany = h / 2.0;
        double fovy = 2 * Math.atan2(tany, tanx) * 180 / Math.PI; // degrees
        // TODO - expose near/far clip planes
        double zNear = 0.3 * camDistScene;
        double zFar = 3.0 * camDistScene;

        GL2Adapter ga = actorContext.getGL2Adapter();
        ga.glMatrixMode(MatrixMode.GL_PROJECTION);
        ga.glLoadIdentity();

        // Distance between the viewers eyes
        double iodPx = cameraScreenGeometry.getIntraocularDistanceCm()
                * cameraScreenGeometry.getScreenPixelsPerCm();
        double iodScene = iodPx / camera.getPixelsPerSceneUnit();
        double frustumShift = -eyeShift * (iodScene/2) * (zNear/camDistScene);        
        double modelTranslation = -eyeShift * iodScene/2; // for right eye

        // TODO - probably don't need both atan2() and tan()...
        double top = zNear * Math.tan(fovy/360*Math.PI);
        double right = aspect * top;
        ga.glFrustum(
                -right + frustumShift, right + frustumShift,
                -top, top,
                zNear, zFar);
        ga.glTranslated(modelTranslation, 0, 0);
        
        ga.glMatrixMode(MatrixMode.GL_MODELVIEW);
    }

}
