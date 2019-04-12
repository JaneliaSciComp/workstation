package org.janelia.workstation.browser.api.services;

import javax.swing.JFrame;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.system.ParentFrame;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provides the parent frame for j-option-pane, etc.
 *
 * @author fosterl
 */
@ServiceProvider(service = ParentFrame.class, path=ParentFrame.LOOKUP_PATH)
public class ConsoleParentFrame implements ParentFrame {

    private static JFrame mainFrame;

    @Override
    public JFrame getMainFrame() {
        if (mainFrame == null) {
            try {
                mainFrame = WindowLocator.getMainFrame();
            }
            catch (Exception ex) {
                FrameworkImplProvider.handleException(ex);
            }
        }
        return mainFrame;
    }
    
}
