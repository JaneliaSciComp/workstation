package org.janelia.it.workstation.browser.api.services;

import java.util.logging.Logger;

import org.janelia.it.jacs.integration.framework.system.ErrorHandler;
import org.janelia.it.workstation.browser.logging.CustomLoggingLevel;
import org.openide.util.lookup.ServiceProvider;

/**
 * This error-handler impl defers to the NetBeans logging framework.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ErrorHandler.class, path=ErrorHandler.LOOKUP_PATH)
public class ConsoleErrorHandler implements ErrorHandler {

    private static final Logger logger = Logger.getLogger(ConsoleErrorHandler.class.getName());
    
    @Override
    public void handleException(Throwable t) {
        logger.log(CustomLoggingLevel.USER_ERROR, null, t);
    }

    @Override
    public void handleException(String message, Throwable t) {
        logger.log(CustomLoggingLevel.USER_ERROR, message, t);
    }

    @Override
    public void handleExceptionQuietly(Throwable t) {
        logger.log(CustomLoggingLevel.USER_WARN, null, t);
    }

    @Override
    public void handleExceptionQuietly(String message, Throwable t) {
        logger.log(CustomLoggingLevel.USER_WARN, message, t);
    }
}
