package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.AnnotationCategory;

import java.util.Collection;

public class AnnotationEvent {
    public enum Type {
        CREATE, DELETE, UPDATE, RENAME, OWNER_CHANGE, RADIUS_UPDATE, SPATIAL_FILTER, REPARENT;
    };

    private Type type;
    private AnnotationCategory category;
    private Collection<TmNeuronMetadata> neurons;
    private Collection<TmGeoAnnotation> vertices;
    private boolean enabled;
    private String description;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
