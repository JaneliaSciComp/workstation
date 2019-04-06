package org.janelia.it.workstation.browser.gui.support;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.MailHelper;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a dialog to get the user's input and then sends an email to JIRA to create a ticket.
 * 
 * @author kimmelr
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MailDialogueBox {

    private static final Logger log = LoggerFactory.getLogger(MailDialogueBox.class);

    private static final String LOG_FILE_NAME = "messages.log";
    
    private final String toEmail = ConsoleProperties.getString("console.HelpEmail");
    private String fromEmail;
    private String subject = "";
    private String initialBody = "";
    private String title = "";
    private String promptText = "";
    private StringBuffer body = new StringBuffer();
    private JFrame parentFrame;

    private MailDialogueBox(JFrame parentFrame, String fromEmail) {
        this.parentFrame = parentFrame;
        this.fromEmail = fromEmail;
    }
    
    public static MailDialogueBox newDialog(JFrame parentFrame, String fromEmail) {
        return new MailDialogueBox(parentFrame, fromEmail);
    }

    public MailDialogueBox withTitle(String title) {
        this.title = title;
        return this;
    }

    public MailDialogueBox withPromptText(String promptText) {
        this.promptText = promptText;
        return this;
    }

    public MailDialogueBox withTextAreaBody(String initialBody) {
        this.initialBody = initialBody;
        return this;
    }
    
    public MailDialogueBox withEmailSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public MailDialogueBox append(String str) {
        body.append(str);
        return this;
    }
    
    public MailDialogueBox appendStandardPrefix() {
        append("\nSubject Key: ").append(AccessManager.getSubjectKey());
        append("\nApplication: ").append(ConsoleApp.getConsoleApp().getApplicationName()).append(" v").append(ConsoleApp.getConsoleApp().getApplicationVersion());
        append("\nOperating System: ").append(SystemInfo.getOSInfo());
        append("\nJava: ").append(SystemInfo.getJavaInfo());
        append("\nRuntime: ").append(SystemInfo.getRuntimeJavaInfo());
        return this;
    }

    public MailDialogueBox appendLine(String str) {
        body.append(str).append("\n");
        return this;
    }
    
    public String showPopup() {
        String desc = null;
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(promptText), BorderLayout.NORTH);
        JTextArea textArea = new JTextArea(4, 40);
        textArea.setText(initialBody);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        int ans = JOptionPane.showConfirmDialog(parentFrame, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans == JOptionPane.CANCEL_OPTION) return null;
        desc = textArea.getText() + "\n";
        return desc;
    }
    
    public void sendEmail() {
        
        // Flush all long handlers so that we have a complete log file
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(""); 
        for (java.util.logging.Handler handler : logger.getHandlers()) {
            handler.flush();
        }
        
        String filename = null;
        File logDir = new File(Places.getUserDirectory(), "var/log");
        File logfile = new File(logDir, LOG_FILE_NAME);
        if (!logfile.canRead()) {
            log.info("Can't read log file at "+logfile.getAbsolutePath());
            logfile = null;
        }
        else {
            filename = AccessManager.getSubjectName()+"_"+LOG_FILE_NAME;
        }
        
        log.info("Sending email from {} to {} with attachment {}",fromEmail,toEmail, logfile);
        
        MailHelper helper = new MailHelper();
        helper.sendEmail(fromEmail, toEmail, subject, body.toString(), logfile, filename);

        // TODO: this should only be shown when the user manually reports a bug
//        JOptionPane.showMessageDialog(
//                FrameworkImplProvider.getMainFrame(), "Bug was reported successfully", "Success",
//                JOptionPane.INFORMATION_MESSAGE);
    }
}
