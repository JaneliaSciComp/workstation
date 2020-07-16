package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSpatialFilter;

import java.util.Collection;

public class NeuronSpatialFilterUpdateEvent extends NeuronUpdateEvent {
    private boolean enabled = false;
    private NeuronSpatialFilter filter;
    private String description;

    public NeuronSpatialFilterUpdateEvent(boolean enabled) {
        this.enabled = enabled;
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

    public NeuronSpatialFilter getFilter() {
        return filter;
    }

    public void setFilter(NeuronSpatialFilter filter) {
        this.filter = filter;
    }
}

