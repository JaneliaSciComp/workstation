package org.janelia.it.workstation.gui.browser.api.services;

import org.janelia.it.jacs.integration.framework.system.ErrorHandler;
import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.openide.util.lookup.ServiceProvider;

/**
 * This error-handler impl defers to the data browser's console app.
 *
 * @author fosterl
 */
@ServiceProvider(service = ErrorHandler.class, path=ErrorHandler.LOOKUP_PATH)
public class ConsoleErrorHandler implements ErrorHandler {

    @Override
    public void handleException(Throwable ex) {
        ConsoleApp.handleException(ex);
    }
    
}
