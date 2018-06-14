package org.janelia.it.workstation.browser.logging;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.IllegalComponentStateException;
import java.io.IOException;
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
        
        // NetBeans bug: https://netbeans.org/bugzilla/show_bug.cgi?id=270487
        if ((e instanceof IllegalComponentStateException) && "component must be showing on the screen to determine its location".equals(e.getMessage())) {
            return true;
        }
        
        // JDK bug: http://bugs.java.com/view_bug.do?bug_id=8003398
        if ((e instanceof IllegalArgumentException) && e.getMessage()!=null && "adding a container to a container on a different GraphicsDevice".equalsIgnoreCase(e.getMessage().trim())) {
            return true;
        }
        
        // JDK bug: http://bugs.java.com/view_bug.do?bug_id=7117595
        if ((e instanceof ArrayIndexOutOfBoundsException) && "1".equals(e.getMessage())) {
            StackTraceElement ste = e.getStackTrace()[0];
            if ("sun.awt.Win32GraphicsEnvironment".equals(ste.getClassName()) && "getDefaultScreenDevice".equals(ste.getMethodName())) {
                return true;
            }
        }
        
        // NetBeans bug: https://netbeans.org/bugzilla/show_bug.cgi?id=232389
        if (e.getClass().equals(IOException.class) && "Cyclic reference. Somebody is trying to get value from FolderInstance (org.openide.awt.Toolbar$Folder) from the same thread that is processing the instance".equals(e.getMessage())) {
            return true;
        }
     
        return false;
    }
}
