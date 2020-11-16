package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.scripts.spatialfilter.NeuronSpatialFilter;

public class NeuronSpatialFilterUpdateEvent extends ViewerEvent {
    private boolean enabled = false;
    private NeuronSpatialFilter filter;
    private String description;

    public NeuronSpatialFilterUpdateEvent( Object source,
                                           boolean enabled,
                                          NeuronSpatialFilter filter, String description) {
        super(source);
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

