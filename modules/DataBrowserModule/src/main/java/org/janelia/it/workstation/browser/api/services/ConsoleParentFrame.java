package org.janelia.it.workstation.browser.api.services;

import javax.swing.JFrame;

import org.janelia.it.jacs.integration.framework.system.ParentFrame;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provides the parent frame for j-option-pane, etc.
 *
 * @author fosterl
 */
@ServiceProvider(service = ParentFrame.class, path=ParentFrame.LOOKUP_PATH)
public class ConsoleParentFrame implements ParentFrame {

    @Override
    public JFrame getMainFrame() {
        return ConsoleApp.getMainFrame();
    }
    
}
