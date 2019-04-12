package org.janelia.workstation.common.logging;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.IllegalComponentStateException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.common.gui.appoptions.ApplicationOptionsPanelController;
import org.janelia.it.workstation.browser.logging.CustomLoggingLevel;
import org.janelia.it.workstation.browser.util.Utils;
import org.netbeans.api.options.OptionsDisplayer;

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

        // Ignore memory issues, these do not represent bugs.
        if (e instanceof OutOfMemoryError || (e.getMessage()!=null && e.getMessage().contains("Java heap space"))) {

            SwingUtilities.invokeLater(() -> {

                Integer maxMem = null;
                try {
                    maxMem = Utils.getMemoryAllocation();
                }
                catch (Exception e1) {
                    logger.log(CustomLoggingLevel.SEVERE, "Error getting memory allocation", e1);
                }
                
                StringBuilder html = new StringBuilder("<html><body>");
                html.append("Workstation has run out of memory. ");
                if (maxMem != null) {
                    html.append("The Workstation is currently allowed "+maxMem+" GB.");
                }
                html.append("<br>You can increase the amount of memory allocated to the Workstation in the Preferences dialog.</body></html>");

                String[] buttons = { "Open Preferences", "Cancel" };
                int selectedOption = JOptionPane.showOptionDialog(FrameworkImplProvider.getMainFrame(), html,
                        "Out of Memory", JOptionPane.YES_NO_OPTION, 0, null, buttons, buttons[0]);

                if (selectedOption == 0) {
                    OptionsDisplayer.getDefault().open(ApplicationOptionsPanelController.PATH);
                }
                
            });
            return true;
        }

        // Ignore all disk space issues, these do not represent bugs.
        if (e.getMessage()!=null && e.getMessage().contains("No space left on device")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                        "<html>There is no space left on the disk you are using.</html>", 
                        "No Space on Disk", JOptionPane.ERROR_MESSAGE);
            });
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
        
        // NetBeans bug: https://netbeans.org/bugzilla/show_bug.cgi?id=270487
        if ((e instanceof IllegalComponentStateException) && "component must be showing on the screen to determine its location".equals(e.getMessage())) {
            return true;
        }
        
        // NetBeans bug: https://netbeans.org/bugzilla/show_bug.cgi?id=232389
        if (e instanceof IOException && "Cyclic reference. Somebody is trying to get value from FolderInstance (org.openide.awt.Toolbar$Folder) from the same thread that is processing the instance".equals(e.getMessage())) {
            return true;
        }
     
        return false;
    }
}
