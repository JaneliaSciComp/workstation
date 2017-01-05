package org.janelia.it.workstation.browser.gui.support;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.janelia.it.jacs.shared.utils.MailHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: kimmelr
 * Date: 5/22/12
 * Time: 2:51 PM
 */
public class MailDialogueBox {

    private static final Logger log = LoggerFactory.getLogger(MailDialogueBox.class);

    private static final String LOG_FILE_NAME = "messages.log";
    
    private final String toEmail = ConsoleProperties.getString("console.HelpEmail");
    private String fromEmail;
    private String subject = "";
    private String initialBody = "";
    private String title = "Problem Description";
    private String promptText = "If possible, please describe what you were doing when the error occured.";
    private String messagePrefix = "";
    private String messageSuffix = "";
    private JFrame parentFrame;

    public MailDialogueBox(JFrame parentFrame, String fromEmail, String subject) {
        this.parentFrame = parentFrame;
        this.fromEmail = fromEmail;
        this.subject = subject;
    }

    public MailDialogueBox(JFrame parentFrame, String fromEmail, String subject, String messagePrefix) {
        this.parentFrame = parentFrame;
        this.fromEmail = fromEmail;
        this.subject = subject;
        this.messagePrefix = messagePrefix;
    }

    public MailDialogueBox(JFrame parentFrame, String fromEmail, String subject, String messagePrefix, String initialBody, String title, String promptText) {
        this.parentFrame = parentFrame;
        this.fromEmail = fromEmail;
        this.subject = subject;
        this.title = title;
        this.promptText = promptText;
        this.initialBody = initialBody;
        this.messagePrefix = messagePrefix;
    }

    public void showPopupThenSendEmail() {
        String desc = null;
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(promptText), BorderLayout.NORTH);
        JTextArea textArea = new JTextArea(4, 40);
        textArea.setText(initialBody);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        int ans;
        while (desc == null || desc.equals("")) {
            ans = JOptionPane.showConfirmDialog(parentFrame, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans == JOptionPane.CANCEL_OPTION) return;
            desc = textArea.getText() +"\n";
        }
        sendEmail(desc);
    }

    public void sendEmail() {
        sendEmail("");
    }
    
    private void sendEmail(String desc) {
        
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
            filename = AccessManager.getUsername()+"_"+LOG_FILE_NAME;
        }
        
        String body = messagePrefix + desc + messageSuffix;
        
        log.info("Sending email from {} to {} with attachment {}",fromEmail,toEmail, logfile);
        
        MailHelper helper = new MailHelper();
        helper.sendEmail(fromEmail, toEmail, subject, body, logfile, filename);
    }
    
    /**
     * This method gives the caller a way to place text at the end of the message.
     * @param messageSuffixInformation Text to display after everything else.
     */
    public void addMessageSuffix(String messageSuffixInformation) {
        this.messageSuffix = messageSuffixInformation;
    }
}
