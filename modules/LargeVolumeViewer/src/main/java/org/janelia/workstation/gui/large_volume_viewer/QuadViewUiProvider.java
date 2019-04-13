package org.janelia.workstation.gui.large_volume_viewer;

import javax.swing.JFrame;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.model.domain.DomainObject;

class QuadViewUiProvider {
    static QuadViewUi createQuadViewUi(JFrame parentFrame, DomainObject initialObject, boolean overrideFrameMenuBar, AnnotationModel annotationModel) {
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
            return new URLBasedQuadViewUi(parentFrame, initialObject, false, annotationModel);
        } else {
            return new FileBasedQuadViewUi(parentFrame, initialObject, true, annotationModel);
        }
    }
}
