package org.janelia.workstation.common.logging;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.util.logging.Logger;

import org.janelia.workstation.core.logging.CustomLoggingLevel;

/**
 * Handle uncaught exceptions in the EDT thread by presenting them to the user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EDTExceptionInterceptor extends EventQueue {

    private static final Logger logger = Logger.getLogger(EDTExceptionInterceptor.class.getName());
    
    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            super.dispatchEvent(event);
        } 
        catch (Throwable throwable) {
            if (ErrorPopups.attemptExceptionHandling(throwable)) {
                // Known harmless issues are logged with lower logging level so as not to bother the user or spam JIRA tickets
                logger.log(CustomLoggingLevel.WARNING, null, throwable);
            }
            else {
                logger.log(CustomLoggingLevel.USER_ERROR, null, throwable);
            }
        }
    }

}
