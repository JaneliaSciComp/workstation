package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

import java.util.Collection;
import java.util.List;

public class AnnotationEvent {
    public enum Type {
        CREATE, DELETE, UPDATE, RENAME, OWNER_CHANGE, RADIUS_UPDATE, REPARENT;
    };

    private Type type;
    private AnnotationCategory category;
    private Collection<TmNeuronMetadata> neurons;
    private Collection<TmGeoAnnotation> vertices;

    public AnnotationEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AnnotationCategory getCategory() {
        return category;
    }

    public void setCategory(AnnotationCategory category) {
        this.category = category;
    }

    public Collection<TmNeuronMetadata> getNeurons() {
        return neurons;
    }

    public void setNeurons(Collection<TmNeuronMetadata> neurons) {
        this.neurons = neurons;
    }

    public Collection<TmGeoAnnotation> getVertices() {
        return vertices;
    }

    public void setVertices(Collection<TmGeoAnnotation> vertices) {
        this.vertices = vertices;
    }

}
