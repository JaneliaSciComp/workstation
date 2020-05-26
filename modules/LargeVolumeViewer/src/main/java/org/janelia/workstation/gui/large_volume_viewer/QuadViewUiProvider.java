package org.janelia.workstation.gui.large_volume_viewer;

import javax.swing.JFrame;

import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.model.domain.DomainObject;

public class QuadViewUiProvider {
    public static QuadViewUi createQuadViewUi(JFrame parentFrame, boolean overrideFrameMenuBar) {
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
            return new URLBasedQuadViewUi(parentFrame, false);
        } else {
            return new FileBasedQuadViewUi(parentFrame, true);
        }
    }
}
