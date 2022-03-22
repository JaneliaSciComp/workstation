package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class ColorModelUpdateEvent extends ViewerEvent {
    ImageColorModel imageColorModel;
    public ColorModelUpdateEvent(Object source,
                                 ImageColorModel colorModel) {
        super(source);
        imageColorModel = colorModel;
    }

    public ImageColorModel getImageColorModel() {
        return imageColorModel;
    }
}
