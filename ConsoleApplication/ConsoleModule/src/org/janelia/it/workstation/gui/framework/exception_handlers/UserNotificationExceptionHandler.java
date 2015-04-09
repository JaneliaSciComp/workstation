package org.janelia.it.workstation.gui.framework.exception_handlers;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.workstation.api.facade.concrete_facade.xml.InvalidXmlException;
import org.janelia.it.workstation.api.facade.facade_mgr.ConnectionStatusException;
import org.janelia.it.workstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.workstation.api.stub.data.FatalCommError;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.MailDialogueBox;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.util.text_component.StandardTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UserNotificationExceptionHandler implements ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationExceptionHandler.class);
    
    public static final String SEND_EMAIL_BUTTON_LABEL = "Send Email to Our Support";
    
    private static final int MAX_POPUP_LINE_LENGTH = 120;
    private static final Object[] normalOptions = {"OK", SEND_EMAIL_BUTTON_LABEL};
    private static final Object defaultOption = normalOptions[0];
    private static final String buildDate = "@@date@@";

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
        if (throwable instanceof InvalidXmlException) {
            presentOutputFrame((InvalidXmlException) throwable);
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
        List messageList = new ArrayList();
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
            MailDialogueBox mailDialogueBox = new MailDialogueBox(SessionMgr.getMainFrame(),
                    (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
                    "Workstation Exception Report",
                    "Problem Description: ");
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\nApplication: ").append(SessionMgr.getSessionMgr().getApplicationName()).append(" v").append(SessionMgr.getSessionMgr().getApplicationVersion());
                sb.append("\nOperating System: ").append(SystemInfo.getOSInfo());
                sb.append("\nJava: ").append(SystemInfo.getJavaInfo());
                sb.append("\nRuntime: ").append(SystemInfo.getRuntimeJavaInfo());
                sb.append("\n\nException:\n");
                sb.append(exception.getClass().getName()).append(": "+exception.getMessage()).append("\n");
                int stackLimit = 100;
                int i = 0;
                for (StackTraceElement element : exception.getStackTrace()) {
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
    
    /**
     * Creates a display frame for parse output if one does not already exist.
     * Then display contents of the output buffer.
     */
    private void presentOutputFrame(InvalidXmlException throwable) {
        
        StringBuffer outputBuffer = throwable.getParseData();
        final JDialog validationOutputDialog = new JDialog(getParentFrame());
        validationOutputDialog.setTitle(throwable.getTitle()+" "+throwable.getFileName());
        validationOutputDialog.setModal(true);
        int width = 600;
        int height = 400;
        validationOutputDialog.setSize(width, height);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int xLoc = ((int) screenSize.getWidth()-width)/2;
        int yLoc = ((int) screenSize.getHeight()-height)/2;
        validationOutputDialog.setLocation(xLoc, yLoc);
        validationOutputDialog.getContentPane().setLayout(new BorderLayout());
        JTextArea validationTA = new StandardTextArea();
        JScrollPane scrollPane = new JScrollPane(validationTA);
        validationOutputDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                validationOutputDialog.setVisible(false);
                validationOutputDialog.dispose();
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(dismissButton);
        validationOutputDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        validationOutputDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                validationOutputDialog.setVisible(false);
                validationOutputDialog.dispose();
            }
        });

        validationTA.setText(outputBuffer.toString());
        validationOutputDialog.setVisible(true);
    }

    private static JFrame getParentFrame() {
        return SessionMgr.getMainFrame();
    }

    private URL getEmailURL() {
        return emailURL;
    }

}
