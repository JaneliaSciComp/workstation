
package org.janelia.scenewindow;

import Jama.Matrix;
import org.janelia.geometry3d.Vector3;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Viewport;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.geom.Vec3;

/**
 *
 * @author brunsc
 */
public abstract class SceneInteractor 
implements MouseListener, MouseMotionListener, MouseWheelListener
{
    protected final AbstractCamera camera;
    protected final Component component;
    private final Vector3 xAxis = new Vector3(1, 0, 0);
    
    public SceneInteractor(AbstractCamera camera, Component component) {
        this.camera = camera;
        this.component = component;
    }
    
    public abstract String getToolTipText();
    
    public void notifyObservers() {
        if (TmModelManager.getInstance().getCurrentSample() == null)
            // no sample, no notifications
            return;

        camera.getVantage().notifyObservers();
        Vantage vantage = camera.getVantage();
        Matrix m2v = TmModelManager.getInstance().getMicronToVoxMatrix();
        Jama.Matrix micLoc = new Jama.Matrix(new double[][]{
                {vantage.getFocus()[0],},
                {vantage.getFocus()[1],},
                {vantage.getFocus()[2],},
                {1.0,},});
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix voxLoc = m2v.times(micLoc);
        Vec3 voxelXyz = new Vec3(
                (float) voxLoc.get(0, 0),
                (float) voxLoc.get(1, 0),
                (float) voxLoc.get(2, 0));
        TmViewState currView = TmModelManager.getInstance().getCurrentView();
        currView.setCameraFocusX(voxelXyz.getX());
        currView.setCameraFocusY(voxelXyz.getY());
        currView.setCameraFocusZ(voxelXyz.getZ());
        currView.setZoomLevel(vantage.getSceneUnitsPerViewportHeight());
    }
    
    public boolean panPixels(int dx, int dy, int dz) {
        if ((dx == 0) && (dy == 0) && (dz == 0))
            return false;
        float scale = camera.getVantage().getSceneUnitsPerViewportHeight() 
                / component.getHeight();
        return panSceneUnits(new Vector3(dx, dy, dz).multiplyScalar(scale));
    }
    
    public boolean panSceneUnits(Vector3 pan) {
        if (pan.lengthSquared() == 0)
            return false;
        Vantage v = camera.getVantage();
        Vector3 newF = new Vector3(v.getFocusPosition());
        // Rotate focus vector to match current view
        Vector3 dF = new Vector3(pan);
        Rotation R = new Rotation(v.getRotationInGround());
        dF.applyRotation(R);
        newF.add(dF);
        return v.setFocusPosition(newF);
    }
    
    private Rotation rotatePixelsRotation(int dx, int dy, float radiansPerScreen) {
        if ((dx == 0) && (dy == 0))
            return null;
        Vector3 axis = new Vector3(-dy, dx, 0); // Rotate about this axis
        float amount = axis.length(); // Longer drag means more rotation
        axis.multiplyScalar(1.0f/amount); // normalize axis length to 1.0
        Viewport vp = camera.getViewport();
        float windowSize = 0.5f * (vp.getHeightPixels() + vp.getWidthPixels());
        amount /= windowSize; // convert pixels to screens
        amount *= radiansPerScreen; // convert screens to radians
        Vantage v = camera.getVantage();
        Rotation newR = new Rotation(v.getRotationInGround());
        newR.multiply(new Rotation().setFromAxisAngle(axis, amount).transpose());

        return newR;
    }
    
    /**
     * Like rotatePixels, but restricts world-Y axis to upper half of camera YZ plane
     * @param dx
     * @param dy
     * @param radiansPerScreen
     * @return true if the camera position changed
     */
    public boolean orbitPixels(int dx, int dy, float radiansPerScreen) 
    {
        if ((dx == 0) && (dy == 0))
            return false;
        
        Viewport vp = camera.getViewport();
        float windowSize = 0.5f * (vp.getHeightPixels() + vp.getWidthPixels());
        float dAzimuth = -dx * radiansPerScreen / windowSize;
        float dElevation = dy * radiansPerScreen / windowSize;
        ConstVector3 upInWorld = camera.getVantage().getUpInWorld();
        Rotation rotAz = new Rotation().setFromAxisAngle(upInWorld, dAzimuth);
        Rotation rotEl = new Rotation().setFromAxisAngle(xAxis, dElevation);
        Rotation newR = new Rotation(camera.getVantage().getRotationInGround());
        newR.multiply(rotEl);
        newR.copy(new Rotation(rotAz).multiply(newR));
        // System.out.println(dAzimuth+", "+dElevation);

        // NOTE: Constraint to up direction has been moved to Vantage class
        
        Vantage v = camera.getVantage();
        // System.out.println(newR);
        return v.setRotationInGround(newR);
    }
    
    public boolean recenterOnMouse(MouseEvent event) {
        Viewport vp = camera.getViewport();
        int centerX = vp.getOriginXPixels() + vp.getWidthPixels() / 2;
        int centerY = vp.getOriginYPixels() + vp.getHeightPixels() / 2;
        int dx = centerX - event.getX();
        int dy = centerY - event.getY();
        return panPixels(-dx, dy, 0);
    }
    

    
    public boolean rotatePixels(int dx, int dy, float radiansPerScreen) {
        Rotation newR = rotatePixelsRotation(dx, dy, radiansPerScreen);
        if (newR == null)
            return false;
        Vantage v = camera.getVantage();
        return v.setRotationInGround(newR);
    }
    
    /**
     * @param zoomOutRatio Zoom factor. Positive number. 1.0 means no change.
     *  0.5 zooms in by a factor of 2. 2.0 zooms out by a factor of 2.
     * @return true if the camera scale changed
     */
    public boolean zoomOut(float zoomOutRatio) {
        Vantage vantage = camera.getVantage();
        float newScale = zoomOutRatio * vantage.getSceneUnitsPerViewportHeight();
        // System.out.println("zoom out "+zoomOutRatio+", "+newScale);
        return vantage.setSceneUnitsPerViewportHeight(newScale);
    }
    
    public boolean zoomMouseWheel(MouseWheelEvent event, float sensitivity) 
    {
	// Use Google maps convention of scroll wheel up to zoom in.
        // (Even though that makes no sense...)
        int notches = event.getWheelRotation();
        float zoomRatio = (float) Math.pow(2.0, notches * sensitivity);
        return zoomOut(zoomRatio);
    }
 
}
