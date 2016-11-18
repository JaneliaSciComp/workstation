package org.janelia.it.workstation.browser.logging;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.util.logging.Logger;

public class EDTExceptionInterceptor extends EventQueue {

    private static final Logger logger = Logger.getLogger(EDTExceptionInterceptor.class.getName());
    
    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            super.dispatchEvent(event);
        } 
        catch (Throwable throwable) {
            logger.log(CustomLoggingLevel.USER_ERROR, null, throwable);
        }
    }
    
    
}
