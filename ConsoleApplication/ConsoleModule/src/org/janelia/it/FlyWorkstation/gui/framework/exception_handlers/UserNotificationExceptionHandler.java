package org.janelia.it.FlyWorkstation.gui.framework.exception_handlers;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml.InvalidXmlException;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.xml.XMLSecurityException;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.ConnectionStatusException;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.FatalCommError;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.MailDialogueBox;
import org.janelia.it.FlyWorkstation.shared.util.FreeMemoryWatcher;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.FlyWorkstation.shared.util.text_component.StandardTextArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

public class UserNotificationExceptionHandler implements ExceptionHandler {

    private Object[] normalOptions = {"OK", "Send Email to Our Support"};
    private Object defaultOption = normalOptions[0];
    private URL emailURL;
    private String buildDate = new String("@@date@@");
    //  private String version = new String (SessionMgr.getSessionMgr().get);
    private JDialog validationOutputDialog = null;
    private JTextArea validationTA = null;

    private static final int MAX_POPUP_LINE_LENGTH = 120;

    public UserNotificationExceptionHandler() {
    }

    public void handleException(Throwable throwable) {
        String msg = throwable.getMessage();
        if (msg != null && msg.indexOf("No Species is not a known species") > -1) {
            displayDatabaseDown(throwable);
            return;
        }
//     if (throwable instanceof InvalidPropertyFormat) {
//       displayBadPropertyFormat(throwable);
//       return;
//     }
     if (throwable instanceof FatalCommError) {
       displayFatalComm(throwable);
       return;
     }
     if (throwable instanceof javax.naming.AuthenticationException || throwable instanceof SecurityException) {
       displayAuthentication(throwable);
       return;
     }
        if (throwable instanceof ConnectionStatusException) {
            displayConnectionStatus(throwable);
            return;
        }
     if (throwable instanceof InvalidXmlException) {
       presentOutputFrame((InvalidXmlException)throwable);
       return;
     }
//     if (throwable instanceof CommandPostconditionException ||
//         throwable instanceof CommandPreconditionException ||
//         throwable instanceof CommandExecutionException) {
//           getOptionPane().showMessageDialog(getParentFrame(),throwable.getMessage(),
//            "Error!!!", JOptionPane.WARNING_MESSAGE);
//           return;
//     }
        displayDialog(throwable);
    }

    private void displayFatalComm(Throwable throwable) {
        Object[] messages = new String[5];
        if (((FatalCommError) throwable).getMachineName() != null) {
            messages[0] = "There has been a problem communicating with the Application Server.";
            messages[1] = "Please try again in several minutes. ";
            messages[2] = "If the problem persists, please contact your Workstation Administrator.";
            messages[3] = "Please tell the administrator that: " + ((FatalCommError) throwable).getMachineName() + " is not responding.";
        }
        else {
            messages[0] = "The Application Server has had difficulty handling your request.";
            messages[1] = "If the problem persists, please contact your Workstation Administrator.";
            messages[2] = "The server message is: " + throwable.getMessage();
        }
        //getParentFrame().setIconImage((new ImageIcon(this.getClass().getResource(System.getProperty("console.WindowCornerLogo"))).getImage()));
        if (getEmailURL() != null) {
            int ans = getOptionPane().showOptionDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, normalOptions, defaultOption);
            if (ans == 1) sendEmail(throwable);
        }
        else {
            getOptionPane().showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
        }
        getParentFrame().getContentPane().removeAll();
    }

    private void displayAuthentication(Throwable throwable) {
        Object[] messages = new String[3];
        messages[0] = "Invalid Login to Application Server.";
        messages[1] = "Please (re)enter your credentials (File->Set Login...)";
        messages[2] = "If the problem persists, please contact your Workstation Administrator.";
        getOptionPane().showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
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
        messages[1] = "<html><font color=black>The application <font color=red>WILL<font color=black> continue without " + "data from that service.";
        messages[2] = throwable.getMessage();
        getOptionPane().showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
    }

    private void displayDatabaseDown(Throwable throwable) {
        Object[] messages = new String[3];
        messages[0] = "The application server is having problems contacting the database.";
        messages[1] = "An email is automatically being sent to our support team about this condition.";
        messages[2] = "Please try again in several minutes";

        if (getEmailURL() != null) {
            sendEmail(throwable, "FlyWorkstation", "Major Error -- Server " + EJBFactory.getAppServerName() + " cannot contact database!");
        }
        getOptionPane().showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
    }

    private void displayDialog(Throwable throwable) {
        List messageList = new ArrayList();
        messageList.add("The system has had an internal error (Exception).");
//     messages[1]="The system will try to recover gracefully, but may need to be restarted.";
        String orgMessage = throwable.getLocalizedMessage();
        if (orgMessage != null) {
            messageList.add("The exception message is: ");
            int currentLength = orgMessage.length();
            int endIndex = 0;
            while (currentLength > MAX_POPUP_LINE_LENGTH) {
                endIndex = Math.max(orgMessage.indexOf(' ', MAX_POPUP_LINE_LENGTH) + 1, MAX_POPUP_LINE_LENGTH);
                if (currentLength == endIndex + 1) endIndex++; //add for period if that is all that is left
                messageList.add(orgMessage.substring(0, endIndex));
                orgMessage = orgMessage.substring(endIndex);
                currentLength = orgMessage.length();
            }
            messageList.add(orgMessage);
        }
        if (getEmailURL() != null) {
            int ans = getOptionPane().showOptionDialog(getParentFrame(), messageList.toArray(), "ERROR!!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, normalOptions, defaultOption);
            if (ans == 1) sendEmail(throwable);
        }
        else {
            getOptionPane().showMessageDialog(getParentFrame(), messageList.toArray(), "ERROR!!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayBadPropertyFormat(Throwable throwable) throws FileNotFoundException {
        Object[] messages = new String[3];
        messages[0] = "The new value of the property that you just attempted to set is invalid for that property.";
        messages[1] = "Please look at the current format and ensure you follow it.";
        if (throwable.getLocalizedMessage() != null)
            messages[2] = "The message from the server is: " + throwable.getLocalizedMessage();

        getParentFrame().setIconImage(Utils.getClasspathImage("flyscope.jpg").getImage());
        getOptionPane().showMessageDialog(getParentFrame(), messages, "ERROR!!", JOptionPane.ERROR_MESSAGE);
    }

    private void sendEmail(Throwable exception) {
        try {

            MailDialogueBox mailDialogueBox = new MailDialogueBox(SessionMgr.getBrowser(),
                    (String) SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL),
                    "Workstation Exception Report",
                    "Problem Description:");
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\nException Message:\n");
                sb.append(exception.getMessage());
                sb.append("\nException Trace:\n");
                int stackLimit = 100;
                int stopCounter = 0;
                for (StackTraceElement element : exception.getStackTrace()) {
                    sb.append(element.toString());
                    sb.append("\n");
                    if (stopCounter>stackLimit) { break; }
                    stopCounter++;
                }
                mailDialogueBox.addMessageSuffix(sb.toString());
            }
            catch (Exception e) {
                // Do nothing if the notification attempt fails.
                e.printStackTrace();
            }
            mailDialogueBox.showPopupThenSendEmail();

//            String emailFrom = null;
//            while (emailFrom == null || emailFrom.equals("")) {
//                emailFrom = JOptionPane.showInputDialog(getParentFrame(), "Please enter your Internet email address.", "E-Mail Address", JOptionPane.QUESTION_MESSAGE);
//                if (emailFrom == null) return;
//            }
//            String desc = null;
//            JPanel panel = new JPanel();
//            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//            panel.add(new JLabel("Please give a quick description of what you were doing, "));
//            panel.add(new JLabel("what you saw at the time of the problem and any useful information."));
//            panel.add(Box.createVerticalStrut(15));
//            JTextArea textArea = new StandardTextArea(4, 20);
//            panel.add(new JScrollPane(textArea));
//            int ans = 0;
//            while (desc == null || desc.equals("")) {
//                ans = getOptionPane().showConfirmDialog(getParentFrame(), panel, "Problem Description", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
//                if (ans == JOptionPane.CANCEL_OPTION) return;
//                desc = textArea.getText();
//            }
//            String line;
//            URL url = getEmailURL();
//            if (url == null) {
//                showEMailFailureDialog();
//                return;
//            }
//            URLConnection connection = url.openConnection();
//            connection.setDoOutput(true);
//            connection.setDoInput(true);
//            PrintStream out = new PrintStream(connection.getOutputStream());
//            out.print("emailFrom=" + URLEncoder.encode(emailFrom, "UTF-8") + "&problemDescription=" + URLEncoder.encode(formMessage(exception, emailFrom, desc), "UTF-8") + "&subject=" + URLEncoder.encode("Workstation Exception Report", "UTF-8"));
//            out.close();
//            connection.getInputStream();
//            // Now we read the response
//            boolean success = false;
//            BufferedReader stream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            while ((line = stream.readLine()) != null && !success) {
//                if (line.indexOf("Message successfully sent") > -1) success = true;
//            }
//            if (success) {
//                getOptionPane().showMessageDialog(getParentFrame(), "Your message was sent " + "to our support staff.");
//            }
//            else {
//                showEMailFailureDialog();
//            }
        }
        catch (Exception ex) {
            showEMailFailureDialog();
        }
    }

    private void sendEmail(Throwable exception, String emailFrom, String subject) {
        try {
            String line;
            URL url = getEmailURL();
            if (url == null) {
                showEMailFailureDialog();
                return;
            }
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            PrintStream out = new PrintStream(connection.getOutputStream());
            out.print("emailFrom=" + URLEncoder.encode(emailFrom, "UTF-8") + "&problemDescription=" + URLEncoder.encode(formMessage(exception, emailFrom, subject), "UTF-8") + "&subject=" + URLEncoder.encode(subject, "UTF-8"));
            out.close();
            connection.getInputStream();
            // Now we read the response
            boolean success = false;
            BufferedReader stream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = stream.readLine()) != null && !success) {
                if (line.indexOf("Message successfully sent") > -1) success = true;
            }
            /*if (success) {
               JOptionPane.showMessageDialog(getParentFrame(),"Your message was sent "+
                 "to our support staff.");
            }
            else {
               showEMailFailureDialog();
            }*/
        }
        catch (Exception ex) {
            showEMailFailureDialog();
        }
    }

    private void showEMailFailureDialog() {
        getOptionPane().showMessageDialog(getParentFrame(), "Your message was NOT able to be sent " + "to our support staff.  Please contact your support representative.");
    }

    private String formMessage(Throwable throwable, String emailFrom, String desc) {
        StringBuffer sb = new StringBuffer(10000);
        String lineSep = System.getProperty("line.separator");
        sb.append("Exception Thrown of class: " + throwable.getClass() + lineSep + lineSep);
        sb.append("User Description: " + lineSep + desc + lineSep + lineSep);
        sb.append("User email: " + emailFrom + lineSep);
        sb.append("Version: " + SessionMgr.getSessionMgr().getApplicationName() + " v" + SessionMgr.getSessionMgr().getApplicationVersion() + lineSep);
        sb.append("Build Date: " + buildDate + lineSep);
        sb.append("Local (Users Time Zone) Date: " + new Date().toString() + lineSep);
        sb.append("Total Memory: " + FreeMemoryWatcher.getFreeMemoryWatcher().getTotalMemory() + lineSep);
        sb.append("Free Memory : " + FreeMemoryWatcher.getFreeMemoryWatcher().getFreeMemory() + lineSep);
        List inUseProtocols = FacadeManager.getInUseProtocolStrings();
        sb.append("In Use Protocols: ");
        for (Iterator it = inUseProtocols.iterator(); it.hasNext(); ) {
            sb.append(it.next());
            if (it.hasNext()) sb.append(",");
        }
        sb.append(lineSep);
        Object[] openDataSources = FacadeManager.getFacadeManager().getOpenDataSources();
        sb.append("Open Data Sources: " + lineSep);
        for (int i = 0; i < openDataSources.length; i++) {
            sb.append(openDataSources[i].toString() + lineSep);
        }
        sb.append(lineSep);
        sb.append("Message: " + throwable.getMessage() + lineSep);
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baOs, true);
        throwable.printStackTrace(pw);
        String stackMessage;
        if (throwable instanceof XMLSecurityException) {
            stackMessage = "Not Applicable";
        }
        else stackMessage = baOs.toString();
        sb.append(lineSep + lineSep + "Stack Trace: " + lineSep + stackMessage + lineSep);
        Properties props = System.getProperties();
        Set propsSet = new TreeSet();
        propsSet.addAll(props.keySet());
        String key;
        sb.append(lineSep + lineSep + lineSep + "System properties:" + lineSep);
        for (Object aPropsSet : propsSet) {
            key = (String) aPropsSet;
            if (!key.equals("console.Password")) {
                sb.append(key + "=" + props.getProperty(key) + lineSep);
            }
        }
        sb.append(lineSep + lineSep + "Have a nice day!!");
        return sb.toString();
    }

    private URL getEmailURL() {
        if (emailURL != null) return emailURL;
        // todo These features expect a bunch of helper jsps.  Fix this.
        String appServer = EJBFactory.getAppServerName();
        if (appServer != null) {
            appServer = "http://" + appServer;
            try {
                emailURL = new URL(appServer + "/broadcast/FeedbackMailer.jsp");
                return emailURL;
            }
            catch (Exception ex) {
                return null;
            } //cannot determine emailServer URL
        }
        return null;
    }

    /**
     * Creates a display frame for parse output if one does not already exist.
     * Then display contents of the output buffer.
     */
    private void presentOutputFrame(InvalidXmlException throwable) {
        StringBuffer outputBuffer = throwable.getParseData();
        if (validationOutputDialog == null) {
            validationOutputDialog = new JDialog(getParentFrame());
            validationOutputDialog.setTitle(throwable.getTitle() + " " + throwable.getFileName());
            validationOutputDialog.setModal(true);
            int width = 600;
            int height = 400;
            validationOutputDialog.setSize(width, height);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int xLoc = ((int) screenSize.getWidth() - width) / 2;
            int yLoc = ((int) screenSize.getHeight() - height) / 2;
            validationOutputDialog.setLocation(xLoc, yLoc);
            validationOutputDialog.getContentPane().setLayout(new BorderLayout());
            validationTA = new StandardTextArea();
            JScrollPane scrollPane = new JScrollPane(validationTA);
            validationOutputDialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
            JButton dismissButton = new JButton("Dismiss");
            dismissButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    terminateDialog();
                } // End method: actionPerformed
            });
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            buttonPanel.add(dismissButton);
            validationOutputDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            validationOutputDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    terminateDialog();
                } // End method: windowClosing
            });
        } // Must create output frame.

        validationTA.setText(outputBuffer.toString());
        validationOutputDialog.setVisible(true);
    } // End method: presentOutputFrame

    /**
     * Call this when the dialog is to go away.
     */
    private void terminateDialog() {
        validationOutputDialog.setVisible(false);
        validationOutputDialog.dispose();
    } // End method

    private JOptionPane getOptionPane() {
        JFrame mainFrame = new JFrame();
        JFrame parent = getParentFrame();
        if (parent != null) mainFrame.setIconImage(getParentFrame().getIconImage());
        JOptionPane optionPane = new JOptionPane();
        mainFrame.getContentPane().add(optionPane);
        return optionPane;
    }

    private JFrame getParentFrame() {
        return SessionMgr.getMainFrame();
    }

}