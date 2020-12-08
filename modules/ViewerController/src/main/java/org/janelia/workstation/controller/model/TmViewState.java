package org.janelia.workstation.controller.model;

import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSelectionSpatialFilter;

import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * this class manages visibilities of annotations as well as
 */
public class TmViewState {
    private NeuronSelectionSpatialFilter neuronFilter;
    private boolean applyFilter;
    private boolean projectReadOnly;
    private Set<Long> hiddenAnnotations;
    private Set<String> hiddenMeshes;
    private Set<Long> nonInteractableAnnotations;
    private Set<Long> neuronRadiusToggle;
    private Set<Long> reviewModeNeurons;

    private double cameraFocusX;
    private double cameraFocusY;
    private double cameraFocusZ;
    private double zoomLevel;
    private float[] cameraRotation;
    private boolean interpolate;
    private static Map<String, ImageColorModel> colorModels;
    private static Map<Long, Color> customNeuronColors;
    private static Map<Long, Long> customNeuronRadii;

    public void init() {
        neuronFilter = new NeuronSelectionSpatialFilter();
        ConcurrentHashMap<Long, Long> threadSafeMap = new ConcurrentHashMap<>();
        hiddenAnnotations = threadSafeMap.newKeySet();
        hiddenMeshes = threadSafeMap.newKeySet();
        nonInteractableAnnotations = threadSafeMap.newKeySet();
        neuronRadiusToggle = threadSafeMap.newKeySet();
        colorModels = new ConcurrentHashMap<>();
        customNeuronColors = new ConcurrentHashMap<>();
        customNeuronRadii = new ConcurrentHashMap<>();
        reviewModeNeurons = threadSafeMap.newKeySet();
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

    public void toggleHidden(Long annId) {
        if (hiddenAnnotations.contains(annId)) {
            removeAnnotationFromHidden(annId);
        } else {
            addAnnotationToHidden(annId);
        }
    }

    public boolean toggleHidden(TmObjectMesh mesh) {
        if (hiddenMeshes.contains(mesh.getName())) {
            removeMeshFromHidden(mesh.getName());
            return false;
        } else {
            addMeshToHidden(mesh.getName());
            return true;
        }
    }

    public void addAnnotationToHidden(Long annId) {
        hiddenAnnotations.add(annId);
    }

    public void removeAnnotationFromHidden(Long annId) {
        hiddenAnnotations.remove(annId);
    }

    public boolean isHidden(Long annId) {
        return hiddenAnnotations.contains(annId);
    }

    public void addMeshToHidden(String meshName) {
        hiddenMeshes.add(meshName);
    }

    public void removeMeshFromHidden(String meshName) {
        hiddenMeshes.remove(meshName);
    }

    public boolean isHidden(String meshName) {
        return hiddenMeshes.contains(meshName);
    }

    public Set<Long> getNonInteractableAnnotations() {
        return nonInteractableAnnotations;
    }

    public void clearNonInteractableAnnotations() {
        nonInteractableAnnotations.clear();
    }

    public void addAnnotationToNonInteractable(Long annId) {
        nonInteractableAnnotations.add(annId);
    }

    public void removeAnnotationFromNonInteractable(Long annId) {
        nonInteractableAnnotations.remove(annId);
    }

    public boolean isNonInteractable(Long annId) {
        return nonInteractableAnnotations.contains(annId);
    }

    public Set<Long> getNeuronRadiusToggle() {
        return neuronRadiusToggle;
    }

    public void clearNeuronRadiusToggle() {
        neuronRadiusToggle.clear();
    }

    public void addAnnotationToNeuronRadiusToggle(Long annId) {
        neuronRadiusToggle.add(annId);
    }

    public void toggleNeuronInteractable(Long annId) {
        if (nonInteractableAnnotations.contains(annId)) {
            removeAnnotationFromNonInteractable(annId);
        } else {
            addAnnotationToNonInteractable(annId);
        }
    }

    public void removeAnnotationFromNeuronRadiusToggle(Long annId) {
        neuronRadiusToggle.remove(annId);
    }

    public boolean isNeuronRadiusToggle(Long annId) {
        return neuronRadiusToggle.contains(annId);
    }

    public void toggleNeuronRadius(Long annId) {
        if (neuronRadiusToggle.contains(annId)) {
            removeAnnotationFromNeuronRadiusToggle(annId);
        } else {
            addAnnotationToNeuronRadiusToggle(annId);
        }
    }

    public Set<Long> getReviewModeNeurons() {
        return reviewModeNeurons;
    }

    public void clearReviewModeNeurons() {
        reviewModeNeurons.clear();
    }

    public void addNeuronToReviewModeNeurons(Long neuronId) {
        reviewModeNeurons.add(neuronId);
    }

    public void removeNeuronFromReviewModeNeurons(Long neuronId) {
        reviewModeNeurons.remove(neuronId);
    }

    public boolean isNeuronInReviewMode(Long neuronId) {
        return reviewModeNeurons.contains(neuronId);
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

    public boolean isProjectReadOnly() {
        return projectReadOnly;
    }

    public void setProjectReadOnly(boolean projectReadOnly) {
        this.projectReadOnly = projectReadOnly;
    }

    private static Color[] neuronColors = {
            Color.red,
            Color.blue,
            Color.green,
            Color.magenta,
            Color.cyan,
            Color.yellow,
            Color.white,
            // I need more colors!  (1, 0.5, 0) and permutations:
            new Color(1.0f, 0.5f, 0.0f),
            new Color(0.0f, 0.5f, 1.0f),
            new Color(0.0f, 1.0f, 0.5f),
            new Color(1.0f, 0.0f, 0.5f),
            new Color(0.5f, 0.0f, 1.0f),
            new Color(0.5f, 1.0f, 0.0f)
    };

    public static Long getRadiusForNeuron(Long neuronID) {
        if (customNeuronRadii.containsKey(neuronID)) {
            return customNeuronRadii.get(neuronID);
        }
        return null;
    }

    public static void setRadiusForNeuron(Long neuronId, Long radius) {
        customNeuronRadii.put(neuronId, radius);
    }

    public static Color getColorForNeuron(Long neuronID) {
        if (customNeuronColors.containsKey(neuronID)) {
            return customNeuronColors.get(neuronID);
        }
        return null;
    }

    public static Color generateNewColor(Long neuronID) {
        Color newColor = neuronColors[(int) (neuronID % neuronColors.length)];
        setColorForNeuron(neuronID, newColor);
        return newColor;
    }

    public static float[] getColorForNeuronAsFloatArray(Long neuronID) {
        TmNeuronMetadata neuron = NeuronManager.getInstance().getNeuronFromNeuronID(neuronID);
        Color color = getColorForNeuron(neuronID);
        if (color == null) {
            if (neuron.getColor()==null) {
                color = generateNewColor(neuronID);
            } else {
                color = neuron.getColor();
            }
        }

        return new float[]{color.getRed() / 255.0f,
                color.getGreen() / 255.0f, color.getBlue() / 255.0f};
    }

    public static void setColorForNeuron(Long neuronId, Color custom) {
        customNeuronColors.put(neuronId, custom);
    }

    public ImageColorModel getColorMode(String key) {
        if (colorModels==null)
            return null;
        return colorModels.get(key);
    }

    public void setColorModel(String key, ImageColorModel model) {
        colorModels.put(key, model);
    }
}
