package org.janelia.workstation.common.logging;

import java.awt.IllegalComponentStateException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.workstation.common.gui.options.ApplicationOptionsPanelController;
import org.janelia.workstation.core.logging.CustomLoggingLevel;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.options.OptionsDisplayer;

/**
 * Known harmless issues can be logged with lower logging level so as not to bother the user or spam JIRA tickets.
 *
 * This class is in charge of triaging exceptions to determine if they are worth reporting.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExceptionTriage {

    private static final Logger logger = Logger.getLogger(ExceptionTriage.class.getName());

    public static boolean isKnownHarmlessIssue(Throwable e) {

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
                    html.append("The Workstation is currently allowed ").append(maxMem).append(" GB.");
                }
                html.append("<br>You can increase the amount of memory allocated to the Workstation in the Preferences dialog.</body></html>");

                String[] buttons = { "Open Preferences", "Cancel" };
                int selectedOption = JOptionPane.showOptionDialog(FrameworkAccess.getMainFrame(), html,
                        "Out of Memory", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, buttons, buttons[0]);

                if (selectedOption == 0) {
                    OptionsDisplayer.getDefault().open(ApplicationOptionsPanelController.PATH);
                }

            });
            return true;
        }

        // Ignore all disk space issues, these do not represent bugs.
        if (e.getMessage()!=null && (e.getMessage().contains("No space left on device") || e.getMessage().contains("There is not enough space on the disk"))) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
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
