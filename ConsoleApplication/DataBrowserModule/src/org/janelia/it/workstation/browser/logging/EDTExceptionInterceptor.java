package org.janelia.it.workstation.browser.logging;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.util.logging.Logger;

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
            if (isKnownHarmlessIssue(throwable)) {
                // Known harmless issues are logged with lower logging level so as not to bother the user or spam JIRA tickets
                logger.log(CustomLoggingLevel.SEVERE, null, throwable);
            }
            else {
                logger.log(CustomLoggingLevel.USER_ERROR, null, throwable);
            }
        }
    }
    
    private boolean isKnownHarmlessIssue(Throwable e) {

        
        // JDK bug: http://bugs.java.com/view_bug.do?bug_id=8003398
        if (e.getClass().equals(IllegalArgumentException.class) && "adding a container to a container on a different GraphicsDevice".equals(e.getMessage())) {
            return true;
        }

        
        return false;
    }
}
