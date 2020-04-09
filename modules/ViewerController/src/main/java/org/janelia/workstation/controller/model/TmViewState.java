package org.janelia.workstation.controller.model;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.spatialfilter.NeuronSelectionSpatialFilter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * this class manages visibilities of annotations as well as
 */
public class TmViewState {
    private NeuronSelectionSpatialFilter neuronFilter;
    private boolean applyFilter;
    private Set<Long> hiddenAnnotations;

    private double cameraFocusX;
    private double cameraFocusY;
    private double cameraFocusZ;
    private double zoomLevel;
    private float[] cameraRotation;
    private boolean interpolate;

    public void init() {
        neuronFilter = new NeuronSelectionSpatialFilter();
        ConcurrentHashMap<Long, Long> threadSafeMap = new ConcurrentHashMap<>();
        hiddenAnnotations = threadSafeMap.newKeySet();
    }

    public boolean getFilter() {
        return applyFilter;
    }

    public void setFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    public NeuronSelectionSpatialFilter getSpatialFilter() {
        return neuronFilter;
    }

    public Set<Long> getHiddenAnnotations() {
        return hiddenAnnotations;
    }

    public void clearHidden() {
        hiddenAnnotations.clear();
    }

    public void addAnnotationToHidden(Long annId) {
        hiddenAnnotations.add(annId);
    }

    public void removeAnnotationFromHidden(Long annId) {
        hiddenAnnotations.remove(annId);
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
