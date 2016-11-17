package org.janelia.it.workstation.browser.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.gui.support.MailDialogueBox;


public class UserNotificationExceptionHandler implements ExceptionHandler {

    private static final Logger logger = Logger.getLogger(UserNotificationExceptionHandler.class.getName());
    
    public static final String SEND_EMAIL_BUTTON_LABEL = "Report This Issue (Recommended)";
        
    @Override
    public void handleException(Throwable throwable) {
        // Delegate to java.util.logging, which is then handled by the NetBeans framework. 
        logger.log(Level.SEVERE, null, throwable);
    }

    public static void sendEmail(Throwable exception) {
        try {
            MailDialogueBox mailDialogueBox = new MailDialogueBox(ConsoleApp.getMainFrame(),
                    (String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.USER_EMAIL),
                    "Workstation Exception Report",
                    "Problem Description: ");
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\nApplication: ").append(ConsoleApp.getConsoleApp().getApplicationName()).append(" v").append(ConsoleApp.getConsoleApp().getApplicationVersion());
                sb.append("\nOperating System: ").append(SystemInfo.getOSInfo());
                sb.append("\nJava: ").append(SystemInfo.getJavaInfo());
                sb.append("\nRuntime: ").append(SystemInfo.getRuntimeJavaInfo());
                if (exception!=null) {
                    sb.append("\n\nException:\n");
                    sb.append(exception.getClass().getName()).append(": "+exception.getMessage()).append("\n");
                    int stackLimit = 100;
                    int i = 0;
                    for (StackTraceElement element : exception.getStackTrace()) {
                        if (element==null) continue;
                        String s = element.toString();
                        if (!StringUtils.isEmpty(s)) {
                            sb.append("at ");
                            sb.append(element.toString());
                            sb.append("\n");
                            if (i++>stackLimit) {
                                break;
                            }
                        }
                    }
                }
                
                mailDialogueBox.addMessageSuffix(sb.toString());
            }
            catch (Exception e) {
                // Do nothing if the notification attempt fails.
                logger.log(Level.WARNING, "Error building exception suffix" , e);
            }
            mailDialogueBox.showPopupThenSendEmail();
        }
        catch (Exception ex) {
            logger.log(Level.WARNING, "Error sending exception email",ex);
            showEmailFailureDialog();
        }
    }

    private static void showEmailFailureDialog() {
        JOptionPane.showMessageDialog(getParentFrame(), "Your message was NOT able to be sent to our support staff.  Please contact your support representative.");
    }
    
    private static JFrame getParentFrame() {
        return ConsoleApp.getMainFrame();
    }
}
