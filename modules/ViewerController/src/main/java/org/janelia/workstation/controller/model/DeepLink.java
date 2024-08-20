package org.janelia.workstation.controller.model;

import org.janelia.geometry3d.Vantage;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

/**
 * This stores a global ID for locating a HC stack, loading the appropriate Sample/Workspace in the stack, and
 * navigating to a specific viewpoint in that Sample/Workspace.
 */
public class DeepLink {
    public String getHortaCloudStack() {
        return hortaCloudStack;
    }

    public void setHortaCloudStack(String hortaCloudStack) {
        this.hortaCloudStack = hortaCloudStack;
    }

    public TmWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(TmWorkspace workspace) {
        this.workspace = workspace;
    }

    public TmSample getSample() {
        return sample;
    }

    public void setSample(TmSample sample) {
        this.sample = sample;
    }

    public TmViewState getViewpoint() {
        return viewpoint;
    }

    public void setViewpoint(TmViewState viewpoint) {
        this.viewpoint = viewpoint;
    }

    String hortaCloudStack;
    TmWorkspace workspace;
    TmSample sample;
    TmViewState viewpoint;
}
