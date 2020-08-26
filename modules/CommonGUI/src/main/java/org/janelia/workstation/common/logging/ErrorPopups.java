package org.janelia.workstation.common.logging;

import org.janelia.workstation.common.gui.dialogs.LoginDialog;
import org.janelia.workstation.common.gui.options.ApplicationOptionsPanelController;
import org.janelia.workstation.core.logging.CustomLoggingLevel;
import org.janelia.workstation.core.logging.ExceptionTriage;
import org.janelia.workstation.core.model.LoginErrorType;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.options.OptionsDisplayer;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * This class uses ExceptionTriage to determine if an exception needs to be reported
 * to the user via a custom popup. This is typically used for explicitly handled exceptions,
 * such as OutOfMemoryError. If an exception can be handled like this, the caller will typically
 * choose to log it as a warning. If the exception cannot be handled here, the caller may need
 * to escalate the exception in another way.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ErrorPopups {

    private static final Logger log = Logger.getLogger(ErrorPopups.class.getName());

    /**
     * Analyzes the given exception and displays any relevant error popups to the user
     * using SwingUtilities.invokeLater. Returns true if the exception has been handled,
     * or false if it needs to be escalated to the user.
     * @param e
     * @return true if the exception has been handled
     */
    public static boolean attemptExceptionHandling(Throwable e) {
        ExceptionTriage.ExceptionCategory exceptionCategory = ExceptionTriage.getExceptionCategory(e);
        switch (exceptionCategory) {
            case AUTH: return authIssue();
            case NETWORK: return networkIssue();
            case FILE_PERMISSIONS: return filePermission(e.getMessage());
            case OUT_OF_MEMORY: return outOfMemory();
            case OUT_OF_DISK: return outOfDisk();
            case UNKNOWN: return false;
            case MISSING_REMOTE_FILE:
            case KNOWN_BUG:
                return true;
        }

        log.warning("New exception category should be handled in org.janelia.workstation.common.logging.ErrorPopups: "+exceptionCategory);
        return false;
    }

    private static boolean authIssue() {
        log.info("Alerting user that there is an authentication issue");
        // Show the login dialog and allow the user to re-authenticate.
        SwingUtilities.invokeLater(() -> LoginDialog.getInstance().showDialog(LoginErrorType.TokenExpiredError));
        return true;
    }

    private static boolean networkIssue() {
        log.info("Alerting user that there is a network issue");
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                "<html>The server is currently unreachable. There may be a <br>"
                        + "network issue, or the system may be down for maintenance.</html>",
                "Network error", JOptionPane.ERROR_MESSAGE));
        return true;
    }

    private static boolean filePermission(String message) {
        log.info("Alerting user that there is a file permission problem");
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                "<html>Encountered a file permission problem when accessing local disk:<br>"
                        + message+"</html>",
                "File permission error", JOptionPane.ERROR_MESSAGE));
        return true;
    }

    private static boolean outOfMemory() {
        log.info("Alerting user that we have run out of memory");
        // Show dialog to allow user to increase their memory setting
        SwingUtilities.invokeLater(() -> {

            Integer maxMem = null;
            try {
                maxMem = Utils.getMemoryAllocation();
            }
            catch (Exception e1) {
                log.log(CustomLoggingLevel.SEVERE, "Error getting memory allocation", e1);
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

    private static boolean outOfDisk() {
        log.info("Alerting user that there is no space left on disk");
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                "<html>There is no space left on the disk you are using.</html>",
                "No Space on Disk", JOptionPane.ERROR_MESSAGE));
        return true;
    }
}
