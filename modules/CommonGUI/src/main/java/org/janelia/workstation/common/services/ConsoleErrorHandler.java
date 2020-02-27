package org.janelia.workstation.common.services;

import org.janelia.workstation.common.logging.ErrorPopups;
import org.janelia.workstation.core.logging.CustomLoggingLevel;
import org.janelia.workstation.integration.api.ErrorHandler;
import org.openide.util.lookup.ServiceProvider;

import java.util.logging.Logger;

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
        handleException(null, t);
    }

    @Override
    public void handleException(String message, Throwable t) {
        if (ErrorPopups.attemptExceptionHandling(t)) {
            logger.log(CustomLoggingLevel.WARNING, message, t);
        }
        else {
            logger.log(CustomLoggingLevel.USER_ERROR, message, t);
        }
    }

    @Override
    public void handleExceptionQuietly(Throwable t) {
        handleExceptionQuietly(null, t);
    }

    @Override
    public void handleExceptionQuietly(String message, Throwable t) {
        logger.log(CustomLoggingLevel.USER_WARN, message, t);
    }
}
