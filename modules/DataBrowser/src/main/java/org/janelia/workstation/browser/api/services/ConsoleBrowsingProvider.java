package org.janelia.workstation.browser.api.services;

import java.util.concurrent.Callable;

import org.janelia.it.jacs.integration.framework.system.BrowsingProvider;
import org.janelia.it.jacs.integration.framework.system.ErrorHandler;
import org.janelia.workstation.browser.gui.components.DomainExplorerTopComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implements
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = BrowsingProvider.class, position=100)
public class ConsoleBrowsingProvider implements BrowsingProvider {

    @Override
    public void refreshExplorer() {
        refreshExplorer(null);
    }

    @Override
    public void refreshExplorer(final Callable<Void> success) {
        DomainExplorerTopComponent.getInstance().refresh(true, true, success);
    }
}
