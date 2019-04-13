package org.janelia.workstation.browser.api.services;

import org.janelia.workstation.browser.gui.components.ProgressTopComponent;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.janelia.workstation.integration.api.ProgressController;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

@ServiceProvider(service = ProgressController.class, path= ProgressController.LOOKUP_PATH)
public class ConsoleProgressController implements ProgressController {

    @Override
    public void showProgressPanel() {
        TopComponent tc = WindowLocator.getByName(ProgressTopComponent.PREFERRED_ID);
        if (tc!=null) {
            tc.open();
            tc.requestVisible();
        }
    }
    
}
