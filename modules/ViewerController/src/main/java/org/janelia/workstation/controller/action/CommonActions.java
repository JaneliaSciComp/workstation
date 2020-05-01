package org.janelia.workstation.controller.action;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class CommonActions {
    private static final Logger log = LoggerFactory.getLogger(CommonActions.class);

    public static void saveQuadViewColorModel() {
        log.info("saveQuadViewColorModel()");
        try {
            if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
                presentError("You must create a workspace to be able to save the color model!", "No workspace");
            }
            else {
                TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();

                //REFACTOR IMAGE COLOR MODEL - messy and unnecessary
                //workspace.setColorModel(ModelTranslation.translateColorModel(quadViewUi.getImageColorModel()));
                log.info("Setting color model: {}",workspace.getColorModel());
                //saveCurrentWorkspace();
            }
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    public static void presentError(String message, String title) throws HeadlessException {
        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }
}
