package org.janelia.it.workstation.browser.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.exceptions.ConnectionStatusException;
import org.janelia.it.workstation.browser.api.exceptions.FatalCommError;
import org.janelia.it.workstation.browser.api.facade.impl.ejb.EJBFactory;
import org.janelia.it.workstation.browser.gui.support.MailDialogueBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserNotificationExceptionHandler implements ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationExceptionHandler.class);
    
    public static final String SEND_EMAIL_BUTTON_LABEL = "Send Email to Our Support";
    
    private static final int MAX_POPUP_LINE_LENGTH = 120;
    private static final Object[] normalOptions = {"OK", SEND_EMAIL_BUTTON_LABEL};
    private static final Object defaultOption = normalOptions[0];
//    private static final String buildDate = "@@date@@";

    private static URL emailURL;
    static {
        String appServer = EJBFactory.getAppServerName();
        if (appServer!=null) {
            try {
                emailURL = new URL("http://"+appServer+"/broadcast/FeedbackMailer.jsp");
            }
            catch (Exception ex) {
                log.error("Error building email URL",ex);
            }
        }
    }
    
    @Override
    public void handleException(Throwable throwable) {
        log.error("Caught exception",throwable);
        if (throwable instanceof FatalCommError) {
            displayFatalComm(throwable);
            return;
        }
        if (throwable instanceof AuthenticationException||throwable instanceof SecurityException) {
            displayAuthentication(throwable);
            return;
        }
        if (throwable instanceof ConnectionStatusException) {
            displayConnectionStatus(throwable);
            return;
        }
        displayDialog(throwable);
    }

    private void displayFatalComm(Throwable throwable) {
        Object[] messages = new String[5];
        if (((FatalCommError) throwable).getMachineName()!=null) {
            messages[0] = "There has been a problem communicating with the server.";
            messages[1] = "Please try again in several minutes. ";
            messages[2] = "If the problem persists, please contact your Workstation Administrator ";
            messages[3] = "and tell them that \""+((FatalCommError) throwable).getMachineName()+"\" is not responding.";
        }
        else {
            messages[0] = "The server has had difficulty handling your request.";
            messages[1] = "If the problem persists, please contact your Workstation Administrator.";
            messages[2] = "The server message is: "+throwable.getMessage();
        }
        //getParentFrame().setIconImage((new ImageIcon(this.getClass().getResource(System.getProperty("console.WindowCornerLogo"))).getImage()));
        if (getEmailURL()!=null) {
            int ans = JOptionPane.showOptionDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, normalOptions, defaultOption);
            if (ans==1) {
                sendEmail(throwable);
            }
        }
        else {
            JOptionPane.showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
        }
        getParentFrame().getContentPane().removeAll();
    }

    private void displayAuthentication(Throwable throwable) {
        Object[] messages = new String[3];
        messages[0] = "Invalid Login to Application Server.";
        messages[1] = "Please (re)enter your credentials (File->Set Login...)";
        messages[2] = "If the problem persists, please contact your Workstation Administrator.";
        JOptionPane.showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
    }

    private void displayConnectionStatus(Throwable throwable) {
        if (throwable instanceof ConnectionStatusException) {
            if (!((ConnectionStatusException) throwable).notifyUser()) {
                System.out.println(throwable.getMessage());
                return;
            }
        }
        Object[] messages = new String[3];
        messages[0] = "There has been a problem communicating with an Information Service.";
        messages[1] = "<html><font color=black>The application <font color=red>WILL<font color=black> continue without "+"data from that service.";
        messages[2] = throwable.getMessage();
        JOptionPane.showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
    }

    private void displayDialog(Throwable throwable) {
        List<String> messageList = new ArrayList<>();
        messageList.add("The system has had an internal error.");
        String orgMessage = throwable.getLocalizedMessage();
        if (orgMessage!=null) {
            messageList.add("The exception message is: ");
            int currentLength = orgMessage.length();
            int endIndex = 0;
            while (currentLength>MAX_POPUP_LINE_LENGTH) {
                endIndex = Math.max(orgMessage.indexOf(' ', MAX_POPUP_LINE_LENGTH)+1, MAX_POPUP_LINE_LENGTH);
                if (currentLength==endIndex+1) {
                    endIndex++; //add for period if that is all that is left
                }
                messageList.add(orgMessage.substring(0, endIndex));
                orgMessage = orgMessage.substring(endIndex);
                currentLength = orgMessage.length();
            }
            messageList.add(orgMessage);
        }
        if (getEmailURL()!=null) {
            int ans = JOptionPane.showOptionDialog(getParentFrame(), messageList.toArray(), "ERROR!!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, normalOptions, defaultOption);
            if (ans==1) {
                sendEmail(throwable);
            }
        }
        else {
            JOptionPane.showMessageDialog(getParentFrame(), messageList.toArray(), "ERROR!!", JOptionPane.ERROR_MESSAGE);
        }
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
                log.error("Error building exception suffix",e);
            }
            mailDialogueBox.showPopupThenSendEmail();
        }
        catch (Exception ex) {
            log.error("Error sending exception email",ex);
            showEmailFailureDialog();
        }
    }

    private static void showEmailFailureDialog() {
        JOptionPane.showMessageDialog(getParentFrame(), "Your message was NOT able to be sent "+"to our support staff.  Please contact your support representative.");
    }
    
    private static JFrame getParentFrame() {
        return ConsoleApp.getMainFrame();
    }

    private URL getEmailURL() {
        return emailURL;
    }
}
