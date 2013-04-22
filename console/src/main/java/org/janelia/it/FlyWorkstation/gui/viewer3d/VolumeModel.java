package org.janelia.it.FlyWorkstation.gui.viewer3d;

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
    private float[] cropCoords = Mip3d.DEFAULT_CROP_COORDS;
    private float gammaAdjustment = 1.0f;
    private float cropOutLevel = Mip3d.DEFAULT_CROPOUT;
    private float[] colorMask = { 1.0f, 1.0f, 1.0f };

    private Collection<UpdateListener> listeners = new ArrayList<UpdateListener>();

    public float[] getCropCoords() {
        return cropCoords;
    }

    public void setCropCoords(float[] cropCoords) {
        this.cropCoords = cropCoords;
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
        listeners.add( listener );
    }

    public synchronized void removeUpdateListener( UpdateListener listener ) {
        listeners.remove( listener );
    }

    public synchronized void removeAllListeners() {
        listeners.clear();
    }

    public static interface UpdateListener {
        void updateVolume();
        void updateRendering();
    }
}
