package org.janelia.workstation.core.util;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ConnectionMgr;
import org.janelia.workstation.core.api.LocalCacheMgr;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a dialog to get the user's input and then sends an email to JIRA to create a ticket.
 * 
 * @author kimmelr
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ErrorReportDialogueBox {

    private static final Logger log = LoggerFactory.getLogger(ErrorReportDialogueBox.class);

    private static final String LOG_FILE_NAME = "messages.log";

    private String subject = "";
    private String initialBody = "";
    private String title = "";
    private String promptText = "";
    private StringBuffer body = new StringBuffer();
    private JFrame parentFrame;

    private ErrorReportDialogueBox(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }
    
    public static ErrorReportDialogueBox newDialog(JFrame parentFrame) {
        return new ErrorReportDialogueBox(parentFrame);
    }

    public ErrorReportDialogueBox withTitle(String title) {
        this.title = title;
        return this;
    }

    public ErrorReportDialogueBox withPromptText(String promptText) {
        this.promptText = promptText;
        return this;
    }

    public ErrorReportDialogueBox withTextAreaBody(String initialBody) {
        this.initialBody = initialBody;
        return this;
    }
    
    public ErrorReportDialogueBox withEmailSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public ErrorReportDialogueBox append(String str) {
        body.append(str);
        return this;
    }
    
    public ErrorReportDialogueBox appendStandardPrefix() {
        append("\nSubject: ").append(AccessManager.getSubjectKey());
        append("\nApplication: ").append(SystemInfo.appName).append(" v").append(SystemInfo.appVersion);
        append("\nServer: ").append(ConnectionMgr.getConnectionMgr().getConnectionString());
        append("\nOperating System: ").append(SystemInfo.getOSInfo());
        append("\nJava: ").append(SystemInfo.getRuntimeJavaInfo());
        append("\nDisk Cache Usage: ").append(LocalCacheMgr.getInstance().getFileCacheGigabyteUsagePercent()+"%");
        append("\nSystem Memory Usage: ").append(SystemInfo.getSystemMemoryUsagePercent()+"%");
        append("\nJVM Memory: ").append(SystemInfo.getJVMMemory());
        append("\nMemory Setting: ");
        if (Utils.getMemoryAllocation()==null) {
            append("default");
        }
        else {
            append(""+Utils.getMemoryAllocation());
        }
        return this;
    }

    public ErrorReportDialogueBox appendLine(String str) {
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

        String fromEmail = ConsoleProperties.getString("console.FromEmail", null);
        if (fromEmail==null) {
            log.error("Cannot send exception report: no value for console.FromEmail is configured.");
            return;
        }

        String toEmail = ConsoleProperties.getString("console.HelpEmail", null);
        if (toEmail==null) {
            log.error("Cannot send exception report: no value for console.HelpEmail is configured.");
            return;
        }

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
        
        log.info("Sending email from {} to {} with attachment {}", fromEmail, toEmail, logfile);
        
        MailHelper helper = new MailHelper();
        helper.sendEmail(fromEmail, toEmail, subject, body.toString(), logfile, filename);

        // TODO: this should only be shown when the user manually reports a bug
//        JOptionPane.showMessageDialog(
//                FrameworkAccess.getMainFrame(), "Bug was reported successfully", "Success",
//                JOptionPane.INFORMATION_MESSAGE);
    }
}
