package org.janelia.workstation.controller;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.eventbus.*;
import java.util.List;

public class SpatialIndexManager {
    private final NeuronVertexSpatialIndex spatialIndex;

    public SpatialIndexManager() {
        spatialIndex = new NeuronVertexSpatialIndex();
        ViewerEventBus.registerForEvents(this);
    }

    public void initialize() {
        spatialIndex.clear();
        spatialIndex.rebuildIndex(NeuronManager.getInstance().getNeuronList());
    }

    public List<TmGeoAnnotation> getAnchorsInMicronArea(double[] p1, double[] p2) {
        return spatialIndex.getAnchorsInMicronArea(p1, p2);
    }

    public List<TmGeoAnnotation> getAnchorClosestToMicronLocation(double[] micronXYZ, int n) {
        return spatialIndex.getAnchorClosestToMicronLocation(micronXYZ, n);
    }

    public TmGeoAnnotation getAnchorClosestToMicronLocation(double[] voxelXYZ) {
        return spatialIndex.getAnchorClosestToMicronLocation(voxelXYZ);
    }

    @Subscribe
    public void annotationAdded(AnnotationCreateEvent event) {
        for (TmGeoAnnotation annotation : event.getAnnotations()) {
            spatialIndex.addToIndex(annotation);
        }
    }

    @Subscribe
    public void annotationDeleted(AnnotationDeleteEvent event) {
        for (TmGeoAnnotation annotation : event.getAnnotations()) {
            spatialIndex.removeFromIndex(annotation);
        }
    }

    @Subscribe
    public void annotationUpdated(AnnotationUpdateEvent event) {
        for (TmGeoAnnotation annotation : event.getAnnotations()) {
            spatialIndex.updateIndex(annotation);
        }
    }

    @Subscribe
    public void neuronCreated(NeuronCreateEvent event) {
        for (TmNeuronMetadata neuron : event.getNeurons()) {
            for (TmGeoAnnotation annotation : neuron.getGeoAnnotationMap().values()) {
                spatialIndex.removeFromIndex(annotation);
            }
            for (TmGeoAnnotation annotation : neuron.getGeoAnnotationMap().values()) {
                spatialIndex.addToIndex(annotation);
            }
        }
    }

    @Subscribe
    public void neuronUpdated(NeuronUpdateEvent event) {
        if (event.getNeurons()==null)
            return;
        for (TmNeuronMetadata neuron : event.getNeurons()) {
            for (TmGeoAnnotation annotation : neuron.getGeoAnnotationMap().values()) {
                spatialIndex.removeFromIndex(annotation);
            }
            for (TmGeoAnnotation annotation : neuron.getGeoAnnotationMap().values()) {
                spatialIndex.addToIndex(annotation);
            }
        }
    }

    @Subscribe
    public void neuronDeleted(NeuronDeleteEvent event) {
        for (TmNeuronMetadata neuron : event.getNeurons()) {
            for (TmGeoAnnotation annotation : neuron.getGeoAnnotationMap().values()) {
                spatialIndex.removeFromIndex(annotation);
            }
        }
    }

    @Subscribe
    public void projectLoaded(LoadProjectEvent event) {
        spatialIndex.clear();
        spatialIndex.rebuildIndex(NeuronManager.getInstance().getNeuronList());
    }

    @Subscribe
    public void neuronSpatialFilterUpdated(NeuronSpatialFilterUpdateEvent event) {
        spatialIndex.clear();
        spatialIndex.rebuildIndex(NeuronManager.getInstance().getNeuronList());
    }

    @Subscribe
    public void projectClosed(UnloadProjectEvent event) {
        spatialIndex.clear();
    }
}
