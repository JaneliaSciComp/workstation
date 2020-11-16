package org.janelia.workstation.controller.eventbus;

public class ViewEvent extends ViewerEvent {
    private double cameraFocusX;
    private double cameraFocusY;
    private double cameraFocusZ;
    private double zoomLevel;
    private float[] cameraRotation; // as quaternion
    private boolean interpolate;

    public ViewEvent(Object source,
                     double cameraFocusX,
             double cameraFocusY,
             double cameraFocusZ,
             double zoomLevel,
             float[] cameraRotation,
             boolean interpolate) {
        super(source);
        this.cameraFocusX = cameraFocusX;
        this.cameraFocusY = cameraFocusY;
        this.cameraFocusZ = cameraFocusZ;
        this.zoomLevel = zoomLevel;
        this.cameraRotation = cameraRotation;
        this.interpolate = interpolate;
    }

    public double getCameraFocusX() {
        return cameraFocusX;
    }
    public double getCameraFocusY() {
        return cameraFocusY;
    }
    public double getCameraFocusZ() {
        return cameraFocusZ;
    }
    public double getZoomLevel() {
        return zoomLevel;
    }
    public float[] getCameraRotation() {
        return cameraRotation;
    }
    public boolean isInterpolate() {
        return interpolate;
    }

}
