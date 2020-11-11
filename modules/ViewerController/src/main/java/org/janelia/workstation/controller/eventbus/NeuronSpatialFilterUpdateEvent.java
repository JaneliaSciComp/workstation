package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSpatialFilter;

import java.util.Collection;

public class NeuronSpatialFilterUpdateEvent {
    private boolean enabled = false;
    private NeuronSpatialFilter filter;
    private String description;

    public NeuronSpatialFilterUpdateEvent( boolean enabled,
                                          NeuronSpatialFilter filter, String description) {
        this.enabled = enabled;
        this.filter = filter;
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public String getDescription() {
        return description;
    }
    public NeuronSpatialFilter getFilter() {
        return filter;
    }
}

