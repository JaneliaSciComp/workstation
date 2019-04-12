package org.janelia.it.workstation.browser.api.services;

import org.janelia.it.jacs.integration.framework.system.ProgressHandler;
import org.janelia.it.workstation.browser.components.ProgressTopComponent;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

@ServiceProvider(service = ProgressHandler.class, path=ProgressHandler.LOOKUP_PATH)
public class ConsoleProgressHandler implements ProgressHandler {

    @Override
    public void showProgressPanel() {
        TopComponent tc = WindowLocator.getByName(ProgressTopComponent.PREFERRED_ID);
        if (tc!=null) {
            tc.open();
            tc.requestVisible();
        }
    }
    
}
