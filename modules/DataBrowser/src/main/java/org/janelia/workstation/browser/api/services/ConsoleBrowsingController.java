package org.janelia.workstation.browser.api.services;

import java.util.concurrent.Callable;

import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.janelia.workstation.integration.api.BrowsingController;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implements
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = BrowsingController.class, position=100)
public class ConsoleBrowsingController implements BrowsingController {

    @Override
    public void refreshExplorer() {
        refreshExplorer(null);
    }

    @Override
    public void refreshExplorer(final Callable<Void> success) {
        DomainExplorerTopComponent.getInstance().refresh(true, true, success);
    }
}
