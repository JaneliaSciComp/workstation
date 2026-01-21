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
 * Creates a dialog to get the user's input and then sends a report to create a ticket,
 * either via email or via GitHub API.
 */
public class ErrorReportDialogueBox {

    private static final Logger log = LoggerFactory.getLogger(ErrorReportDialogueBox.class);

    private static final String LOG_FILE_NAME = "messages.log";

    // for GitHub reporting
    private static final String ISSUES_BRANCH = "issues";
    private static final String ATTACHMENTS_FOLDER = "attachments";

    private static final String SUBJECT_PREFIX = "[JW] ";

    private String subject = "";
    private String initialBody = "";
    private String title = "";
    private String promptText = "";
    private StringBuffer body = new StringBuffer();
    private JFrame parentFrame;

    // flag to track whether we've reported an error about reporting errors already
    private static boolean silenceErrorReportingFailure = false;

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
    
    public ErrorReportDialogueBox withSubject(String subject) {
        if (!subject.startsWith(SUBJECT_PREFIX)) {
            subject = SUBJECT_PREFIX + subject;
        }
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

    public void sendReport() {
        String method = ConsoleProperties.getString("console.ErrorReportingMethod", null);
        if (method == null) {
            // "email" was our original implementation, so if unset, default to "email" for
            //  backward compatibility
            method = "email";
        }

        if (method.equals("email")) {
            sendEmail();
        } else if (method.equals("github")) {
            sendGitHub();
        } else {
            log.error("Cannot send error report; unknown value {} for console.ErrorReportingMethod", method);
            return;
        }
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

        String filename = "";
        File logfile = getLogfile();
        if (logfile != null) {
            filename = AccessManager.getSubjectName()+"_"+LOG_FILE_NAME;
        }

        log.info("Sending email from {} to {} with attachment {}", fromEmail, toEmail, logfile);
        
        MailHelper helper = new MailHelper();
        boolean result = helper.sendEmail(fromEmail, toEmail, subject, body.toString(), logfile, filename);
        if (!result) {
            String message = "Error reporting email not sent or error in sending email";
            reportErrorReportingFailure(message);
        }
    }

    public void sendGitHub() {
        String githubURL = ConsoleProperties.getString("console.GitHubErrorProjectURL", null);
        if (githubURL==null) {
            log.error("Cannot send exception report: no value for console.GitHubErrorProjectURL is configured.");
            return;
        }
        String githubToken = ConsoleProperties.getString("console.GitHubErrorProjectAccessToken", null);
        if (githubToken==null) {
            log.error("Cannot send exception report: no value for console.GitHubErrorProjectAccessToken is configured.");
            return;
        }

        File logfile = getLogfile();
        log.info("Creating GitHub issue in project {} with logfile attachment {}", githubURL, logfile);

        // note that our logfiles are far too long for a GitHub issue (65k char limit),
        //  so this is a three step process: create issue with body text, upload the
        //  logfile to the repo, then create a comment with a link to the logfile

        GitHubRestClient client = new GitHubRestClient();

        int issueNumber = client.createIssue(subject, body.toString());
        if (issueNumber <= 0) {
            String message = "GitHub issue not created for error report";
            log.error(message);
            reportErrorReportingFailure(message);
            return;
        }

        String path = ATTACHMENTS_FOLDER + "/issue-" + issueNumber + "-" + LOG_FILE_NAME;
        String permalink = client.uploadLogFile(ISSUES_BRANCH, logfile, path);
        if (permalink.isEmpty()) {
            String message = "Logfile not uploaded to GitHub or error in generating permalink";
            log.error(message);
            reportErrorReportingFailure(message);
            return;
        }

        String comment = "[Link to uploaded log file.](" + permalink + ")";
        boolean success = client.addComment(issueNumber, comment);
        if (!success) {
            String message = "Failed to add comment to GitHub issue with permalink to log.";
            log.error(message);
            reportErrorReportingFailure(message);
        }

    }

    private File getLogfile() {

        // Flush all long handlers so that we have a complete log file
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
        for (java.util.logging.Handler handler : logger.getHandlers()) {
            handler.flush();
        }

        File logDir = new File(Places.getUserDirectory(), "var/log");
        File logfile = new File(logDir, LOG_FILE_NAME);
        if (!logfile.canRead()) {
            log.info("Can't read log file at " + logfile.getAbsolutePath());
            logfile = null;
        }

        return logfile;
    }

    private void reportErrorReportingFailure(String message) {
        if (silenceErrorReportingFailure) {
            return;
        }

        Object[] buttons = {"Silence", "Continue"};
        message = "Error reporting failed:\n\n" + message +
            "\n\nPlease report this issue to the site admins, as it cannot be reported automatically!" +
            "\n\nContinue to show this dialog when error reporting fails, or Silence these dialogs for this session?";
        Object response = JOptionPane.showOptionDialog(null,
            message,
            "Error not reported!",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            buttons,
            buttons[0]
        );
        int index = (int) response;
        if (index < 0 || index >= buttons.length) {
            return;
        }
        if (buttons[index].equals("Silence")) {
            silenceErrorReportingFailure = true;
        }
    }
}
