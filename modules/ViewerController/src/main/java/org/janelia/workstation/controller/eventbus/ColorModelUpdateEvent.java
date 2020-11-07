package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class ColorModelUpdateEvent extends WorkspaceEvent {
    public ColorModelUpdateEvent(TmWorkspace workspace, ImageColorModel colorModel) {
        imageColorModel = colorModel;
        setWorkspace(workspace);
    }

    ImageColorModel imageColorModel;
    public ImageColorModel getImageColorModel() {
        return imageColorModel;
    }
}
