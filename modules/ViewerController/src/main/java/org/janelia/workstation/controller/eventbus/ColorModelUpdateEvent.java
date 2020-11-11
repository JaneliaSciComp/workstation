package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class ColorModelUpdateEvent extends WorkspaceEvent {
    ImageColorModel imageColorModel;
    public ColorModelUpdateEvent(TmWorkspace workspace, ImageColorModel colorModel) {
        super(workspace, workspace.getId());
        imageColorModel = colorModel;
    }

    public ImageColorModel getImageColorModel() {
        return imageColorModel;
    }
}
