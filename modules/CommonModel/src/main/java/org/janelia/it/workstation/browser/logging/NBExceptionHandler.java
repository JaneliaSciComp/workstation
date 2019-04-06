package org.janelia.it.workstation.browser.logging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.exceptions.AuthenticationException;
import org.janelia.it.workstation.browser.util.MailDialogueBox;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.RateLimiter;

/**
 * Override NetBeans' exception handling to tie into the workstation's error handler.
 *
 * When the application starts up, this exception handler must be registered like this:
 *   java.util.logging.Logger.getLogger("").addHandler(new NBExceptionHandler());
 *
 * This is implemented according to the advice given on the official NetBeans documentation, here:
 * http://wiki.netbeans.org/DevFaqCustomizingUnexpectedExceptionDialog
 * 
 * However, it has some major flaws, mainly related to the fact that the Throwable being handled is the last 
 * published Throwable, not the Throwable that the user has selected on using the Prev/Next buttons. If 
 * an exception happens asynchronously after the dialog shows up, then the throwable will be updated as well.
 * In general it's a very shoddy solution, but it's currently the best we can do without rewriting all the 
 * NetBeans error handling. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NBExceptionHandler extends Handler implements Callable<JButton>, ActionListener {

    private static final Logger log = LoggerFactory.getLogger(NBExceptionHandler.class);

    private static final int COOLDOWN_TIME_SEC = 300; // Allow one auto exception report every 5 minutes
    private static final int MAX_STACKTRACE_CACHE_SIZE = 1000; // Keep track of a max number of unique stacktraces
    private static final boolean AUTO_SEND_EXCEPTIONS = ConsoleProperties.getBoolean("console.AutoSendExceptions", false);

    // email address from which automated reports to the issue tracker will originate;
    //  this address has an account in JIRA that has right permissions to create tickets
    public static final String REPORT_EMAIL = ConsoleProperties.getString("console.FromEmail");

    private final HashFunction hf = Hashing.md5();
    
    private boolean notified = false;
    
    private Throwable throwable;
    private JButton newFunctionButton;

    private final Multiset<String> exceptionCounts = ConcurrentHashMultiset.create();
    private final RateLimiter rateLimiter = RateLimiter.create(1);
    
    @Override
    public void publish(LogRecord record) {
        if (record.getThrown()!=null) {
            this.throwable = record.getThrown();
            
            // Only auto-send exceptions which are logged at error ("SEVERE") level or higher
            if (record.getLevel().intValue() < Level.SEVERE.intValue()) return;
            
            // JW-25430: Only attempt to auto-send exceptions once the user has logged in
            if (!AccessManager.loggedIn()) return; 
            
            try {
                autoSendNovelExceptions();
            }
            catch (Throwable t) {
                log.error("Error attempting to auto-send exceptions", t);
            }
        }
    }
    
    /**
     * If an exception has not appeared before during this session, go ahead and create a JIRA ticket for it. 
     */
    private synchronized void autoSendNovelExceptions() {

        if (SystemInfo.isDev) {
            return;
        }
        
        if (!AUTO_SEND_EXCEPTIONS) {
            if (!notified) {
                notified = true;
                log.warn("Auto-sending exceptions is not configured. To configure auto-send, set console.AutoSendExceptions=true in console.properties.");
            }
            return;
        }

        String st = ExceptionUtils.getStackTrace(throwable);
        String firstLine = getSummary(st);

        if (isIgnoredForAutoSend(throwable, st)) {
            log.trace("Ignoring exception for auto-send: {}", firstLine);
            return;
        }
        
        // We remove the first line before checking for uniqueness. 
        // Ignoring the exception message when looking for duplicates can cause some false positives, but it's better than a flood. 
        // Even if we miss a novel exception, if it matters then sooner or later it will come up again. 
        String trace = getTrace(st);
        String traceHash = hf.newHasher().putString(trace, Charsets.UTF_8).hash().toString();
        log.trace("Got exception hash: {}",traceHash);
        
        if (!exceptionCounts.contains(traceHash)) {  
            // First appearance of this stack trace, let's try to send it to JIRA.

            // Allow one exception report every cooldown cycle. Our RateLimiter allows one access every 
            // second, so we need to acquire a cooldown's worth of locks.
            if (!rateLimiter.tryAcquire(COOLDOWN_TIME_SEC, 0, TimeUnit.SECONDS)) {
                log.warn("Exception reports exceeded email rate limit. Omitting auto-send of: {}", firstLine);
                return;
            }
            sendEmail(st, false);
        }
        else {
            int count = exceptionCounts.count(traceHash);
            if (count % 10 == 0) {
                log.warn("Exception count reached {} for: {}", count, firstLine);
            }
        }

        exceptionCounts.add(traceHash); // Increment counter

        // Make sure we're not devoting too much memory to stack traces
        if (exceptionCounts.size()>MAX_STACKTRACE_CACHE_SIZE) {
            log.info("Purging single exceptions (cacheSize={})", exceptionCounts.size());
            // Try purging all singleton exceptions.
            for (Iterator<String> i = exceptionCounts.iterator(); i.hasNext();) {
                if (exceptionCounts.count(i.next())<2) {
                    i.remove();
                }
            }
            // Still too big? Just clear it out.
            if (exceptionCounts.size()>MAX_STACKTRACE_CACHE_SIZE) {
                log.info("Clearing exception cache (cacheSize={})", exceptionCounts.size());
                exceptionCounts.clear();
            }
        }   
    }

    /**
     * Filter exceptions which should never be reported to JIRA. These are exceptions for
     * which we can not do anything. They are not caused by bugs in our software, but issues with 
     * the environment or an acceptable user action which happens to generate an exception. 
     * In cases where this method reports false positives and ignores actual problems, the user
     * can still report them manually. 
     */
    private boolean isIgnoredForAutoSend(Throwable throwable, String stacktrace) {
        
        // Ignore auth issues
        if (stacktrace.contains(AuthenticationException.class.getName()+": Invalid username or password")) {
            return true;
        }
        
        // Ignore all broken pipes, because these are usually caused by user initiated cancellation or network issues.
        if (stacktrace.contains("Caused by: java.io.IOException: Broken pipe")) {
            return true;
        }
        
        // Ignore network and data issues. If it's in fact a problem on our end, the user will let us know. 
        if (stacktrace.contains("java.io.IOException: unexpected end of stream") 
                || stacktrace.contains("java.net.ConnectException: Connection refused")
                || stacktrace.contains("java.net.ConnectException: Connection timed out")) {
            return true;
        }
        
        // Ignore older ArtifactDescriptor deserialization issues. These older ArtifactDescriptors are no longer usable, but the user can overwrite them with new preferences.
        if (stacktrace.contains("com.fasterxml.jackson.databind.JsonMappingException: Unexpected token") && stacktrace.contains("ArtifactDescriptor")) {
            return true;
        }
        
        // Ignore problems with local disks 
        if (throwable instanceof java.nio.file.AccessDeniedException) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        this.throwable = null;
    }

    // Return the button we want to be displayed in the Uncaught Exception Dialog.
    @Override
    public JButton call() throws Exception {
        if (newFunctionButton==null) {
            newFunctionButton = new JButton("Report This Issue");
            newFunctionButton.addActionListener(this);
        }
        return newFunctionButton;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.windowForComponent(newFunctionButton).setVisible(false);
        // Due to the way the NotifyExcPanel works, this might not be the exception the user is currently looking at! 
        // Maybe it's better than nothing if it's right 80% of the time? 
        sendEmail(ExceptionUtils.getStackTrace(throwable), true);
    }
    
    private void sendEmail(String stacktrace, boolean askForInput) {

        try {
            String firstLine = getSummary(stacktrace);
            log.info("Reporting exception: "+firstLine);

            String version = ConsoleApp.getConsoleApp().getApplicationVersion();
            String titleSuffix = " from "+AccessManager.getSubjectName()+" -- "+version+" -- "+firstLine;
            String subject = (askForInput?"User-reported Exception":"Auto-reported Exception")+titleSuffix;
             
            MailDialogueBox mailDialogueBox = MailDialogueBox.newDialog(FrameworkImplProvider.getMainFrame(), REPORT_EMAIL)
                    .withTitle("Create A Ticket")
                    .withPromptText("If possible, please describe what you were doing when the error occurred:")
                    .withEmailSubject(subject)
                    .appendStandardPrefix();
            
            if (askForInput) {
                String problemDesc = mailDialogueBox.showPopup();
                if (problemDesc==null) {
                    // User pressed cancel
                    return;
                }
                else {
                    mailDialogueBox.append("\n\nProblem Description:\n");
                    mailDialogueBox.append(problemDesc);
                }
            }

            if (stacktrace!=null) {
                mailDialogueBox.append("\n\nStack Trace:\n");
                mailDialogueBox.append(stacktrace);
            }
            
            mailDialogueBox.sendEmail();
        }
        catch (Exception ex) {
            log.warn("Error sending exception email",ex);
            if (askForInput) { // JW-25430: Only show this message if the email was initiated by the user
                JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                        "Your message was NOT able to be sent to our support staff.  "
                        + "Please contact your support representative.", "Error sending email", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private String getSummary(String st) {
        String[] s = st.split("\n");
        String summary = s[0];
        if (!summary.contains(" ") && s.length > 1) {
            // No message available, add the second line for context
            summary += " "+s[1];
        }
        return summary;
    }

    private String getTrace(String st) {
        int n = st.indexOf('\n');
        if (n+1>st.length()) return st; 
        return st.substring(n+1);
    }
}
