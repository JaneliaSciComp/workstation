package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.DomainObject;
import java.util.List;

public class ViewEvent {

    public enum Type {
        SET, RESET, CLOSE;
    };
    private Type type;
    private double cameraFocusX;
    private double cameraFocusY;
    private double cameraFocusZ;
    private double zoomLevel;
    private float[] cameraRotation; // as quaternion
    private boolean interpolate;

    public ViewEvent() {
    }
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public double getCameraFocusX() {
        return cameraFocusX;
    }

    public void setCameraFocusX(double cameraFocusX) {
        this.cameraFocusX = cameraFocusX;
    }

    public double getCameraFocusY() {
        return cameraFocusY;
    }

    public void setCameraFocusY(double cameraFocusY) {
        this.cameraFocusY = cameraFocusY;
    }

    public double getCameraFocusZ() {
        return cameraFocusZ;
    }

    public void setCameraFocusZ(double cameraFocusZ) {
        this.cameraFocusZ = cameraFocusZ;
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public float[] getCameraRotation() {
        return cameraRotation;
    }

    public void setCameraRotation(float[] cameraRotation) {
        this.cameraRotation = cameraRotation;
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
}
