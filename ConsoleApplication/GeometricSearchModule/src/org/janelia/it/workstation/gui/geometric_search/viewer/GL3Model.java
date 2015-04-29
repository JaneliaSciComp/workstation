package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicCamera3d;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by murphys on 4/10/15.
 */
public class GL3Model {

    public static final float[] DEFAULT_BACKGROUND_COLOR = {0.0f, 0.0f, 0.0f};
    public static final boolean DEFAULT_SHOWING_AXES = true;

    private Vec3 cameraDepth;
    private Camera3d camera3d;
    private float[] backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private float[] voxelMicrometers;
    private int[] voxelDimensions;
    private boolean showAxes = DEFAULT_SHOWING_AXES;

    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 1.0;


    public interface UpdateListener {
        void updateModel();

        void updateRenderer();
    }

    private Collection<UpdateListener> listeners = new ArrayList<>();

    public GL3Model() {
        camera3d = new BasicObservableCamera3d();
        camera3d.setFocus(0.0,0.0,0.0);
        cameraDepth = new Vec3(0.0, 0.0, DEFAULT_CAMERA_FOCUS_DISTANCE);
    }

    /** This may be useful for situations like the HUD, which retains a reference to
     * the volume model across invocations.  Call this prior to reset.
     */
    public void resetToDefaults() {
    }

    /** Volume update means "must refresh actual data being shown." */
    public void setModelUpdate() {
        Collection<UpdateListener> currentListeners;
        synchronized (this) {
            currentListeners = new ArrayList<>( listeners );
        }
        for ( UpdateListener listener: currentListeners ) {
            listener.updateModel();
        }
    }

    /** Render Update means "must refresh data that affects how the primary data is rendered." */
    public void setRenderUpdate() {
        Collection<UpdateListener> currentListeners;
        synchronized (this) {
            currentListeners = new ArrayList<>( listeners );
        }
        for ( UpdateListener listener: currentListeners ) {
            listener.updateRenderer();
        }
    }

    /** Listener management methods. */
    public synchronized void addUpdateListener( UpdateListener listener ) {
        listeners.add(listener);
    }

    public synchronized void removeUpdateListener( UpdateListener listener ) {
        listeners.remove(listener);
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

    /** Convenience method to corral this calculation for consistent use. */
    public double getCameraFocusDistance() {
        if ( cameraDepth == null ) {
            return 1.0;
        }
        return cameraDepth.getZ();
    }

    /** Convenience method to corral this calculation for consistent use. */
    public void setCameraPixelsPerSceneUnit( double screenPixelDistance, double cameraFocusDistance ) {
        if ( getCamera3d() == null ) {
            return;
        }
        getCamera3d().setPixelsPerSceneUnit( Math.abs( screenPixelDistance / cameraFocusDistance ) );
    }

    public void setCamera3d(Camera3d camera3d) {
        this.camera3d = camera3d;
    }

    public Vec3 getCameraDepth() {
        return cameraDepth;
    }

    public void setCameraDepth(Vec3 cameraDepth) {
        this.cameraDepth = cameraDepth;
    }

    public float[] getVoxelMicrometers() {
        return voxelMicrometers;
    }

    public void setVoxelMicrometers(float[] voxelMicrometers) {
        this.voxelMicrometers = voxelMicrometers;
    }

    public int[] getVoxelDimensions() {
        return voxelDimensions;
    }

    public void setVoxelDimensions(int[] voxelDimensions) {
        this.voxelDimensions = voxelDimensions;
    }


    /**
     * @return the backgroundColor
     */
    public float[] getBackgroundColorFArr() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isShowAxes() {
        return showAxes;
    }

    /**
     * Setting this true allows axes to be visible; false hides them.
     *
     * @param showAxes the showAxes to set
     */
    public void setShowAxes(boolean showAxes) {
        this.showAxes = showAxes;
    }


}
