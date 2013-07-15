package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_export.CropCoordSet;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/18/13
 * Time: 5:17 PM
 *
 * Use of this object allows values to be set for the use of the Volume Brick, but while preserving
 * the GLActor abstraction.
 */
public class VolumeModel {
    public static final float DEFAULT_GAMMA_ADJUSTMENT = 1.0f;
    public static final Vec3 DEFAULT_FOCUS_IN_GROUND = new Vec3(0, 0, 0);
    public static final float[] DEFAULT_COLOR_MASK = {1.0f, 1.0f, 1.0f};

    private CropCoordSet cropCoordSet = CropCoordSet.getDefaultCropCoordSet();
    private float gammaAdjustment = DEFAULT_GAMMA_ADJUSTMENT;
    private float cropOutLevel = Mip3d.DEFAULT_CROPOUT;
    private Camera3d camera3d;
    private Vec3 focusInGround = DEFAULT_FOCUS_IN_GROUND;
    private float[] colorMask = DEFAULT_COLOR_MASK;

    private Collection<UpdateListener> listeners = new ArrayList<UpdateListener>();

    /** This may be useful for situations like the HUD, which retains a reference to
     * the volume model across invocations.  Call this prior to reset.
     */
    public void resetToDefaults() {
        cropCoordSet = CropCoordSet.getDefaultCropCoordSet();
        gammaAdjustment = DEFAULT_GAMMA_ADJUSTMENT;
        cropOutLevel = Mip3d.DEFAULT_CROPOUT;
        focusInGround = DEFAULT_FOCUS_IN_GROUND;
        colorMask = DEFAULT_COLOR_MASK;
    }

    public CropCoordSet getCropCoords() {
        return cropCoordSet;
    }

    public void setCropCoords(CropCoordSet cropCoordSet) {
        this.cropCoordSet = cropCoordSet;
    }

    public float getGammaAdjustment() {
        return gammaAdjustment;
    }

    public void setGammaAdjustment(float gammaAdjustment) {
        this.gammaAdjustment = gammaAdjustment;
    }

    public float getCropOutLevel() {
        return cropOutLevel;
    }

    public void setCropOutLevel(float cropOutLevel) {
        this.cropOutLevel = cropOutLevel;
    }

    public float[] getColorMask() {
        return colorMask;
    }

    public void setColorMask(float[] colorMask) {
        this.colorMask = colorMask;
    }

    /** Volume update means "must refresh actual data being shown." */
    public void setVolumeUpdate() {
        Collection<UpdateListener> currentListeners;
        synchronized (this) {
            currentListeners = new ArrayList<UpdateListener>( listeners );
        }
        for ( UpdateListener listener: currentListeners ) {
            listener.updateVolume();
        }
    }

    /** Render Update means "must refresh data that affects how the primary data is rendered." */
    public void setRenderUpdate() {
        Collection<UpdateListener> currentListeners;
        synchronized (this) {
            currentListeners = new ArrayList<UpdateListener>( listeners );
        }
        for ( UpdateListener listener: currentListeners ) {
            listener.updateRendering();
        }
    }

    /** Listener management methods. */
    public synchronized void addUpdateListener( UpdateListener listener ) {
        listeners.add(listener);
    }

    public synchronized void removeUpdateListener( UpdateListener listener ) {
        listeners.remove( listener );
    }

    public synchronized void removeAllListeners() {
        listeners.clear();
    }

    public Camera3d getCamera3d() {
        if ( camera3d == null ) {
            camera3d = new BasicCamera3d();
        }
        return camera3d;
    }

    public void setCamera3d(Camera3d camera3d) {
        this.camera3d = camera3d;
    }

    public Vec3 getFocusInGround() {
        return focusInGround;
    }

    public void setFocusInGround(Vec3 focusInGround) {
        this.focusInGround = focusInGround;
    }

    public static interface UpdateListener {
        void updateVolume();
        void updateRendering();
    }
}
