package org.janelia.workstation.gui.large_volume_viewer;

import javax.swing.JFrame;

import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.model.domain.DomainObject;

public class QuadViewUiProvider {
    public static QuadViewUi createQuadViewUi(JFrame parentFrame, DomainObject initialObject, boolean overrideFrameMenuBar, NeuronManager annotationModel, JadeServiceClient jadeServiceClient) {
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
            return new URLBasedQuadViewUi(parentFrame, initialObject, false, annotationModel, jadeServiceClient);
        } else {
            return new FileBasedQuadViewUi(parentFrame, initialObject, true, annotationModel);
        }
    }
}
