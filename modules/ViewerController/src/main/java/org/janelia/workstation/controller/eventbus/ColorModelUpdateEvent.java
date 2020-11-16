package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class ColorModelUpdateEvent extends WorkspaceEvent {
    ImageColorModel imageColorModel;
    public ColorModelUpdateEvent(Object source,
                                 TmWorkspace workspace,
                                 ImageColorModel colorModel) {
        super(source, workspace, workspace.getId());
        imageColorModel = colorModel;
    }

    public ImageColorModel getImageColorModel() {
        return imageColorModel;
    }
}
