package org.janelia.workstation.common.services;

import javax.swing.JFrame;

import org.janelia.workstation.integration.api.FrameModel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provides the parent frame for j-option-pane, etc.
 *
 * @author fosterl
 */
@ServiceProvider(service = FrameModel.class, path= FrameModel.LOOKUP_PATH)
public class ConsoleFrameModel implements FrameModel {

    private static JFrame mainFrame;

    @Override
    public JFrame getMainFrame() {
        if (mainFrame == null) {
            try {
                mainFrame = WindowLocator.getMainFrame();
            }
            catch (Exception ex) {
                FrameworkAccess.handleException(ex);
            }
        }
        return mainFrame;
    }
    
}
