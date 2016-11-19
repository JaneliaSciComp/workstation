package org.janelia.it.workstation.browser.logging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.gui.support.MailDialogueBox;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    public static final String SEND_EMAIL_BUTTON_LABEL = "Report This Issue (Recommended)";        
    
    private Throwable throwable;
    private JButton newFunctionButton;

    @Override
    public void publish(LogRecord record) {
        if (record.getThrown()!=null) {
            this.throwable = record.getThrown();
        }
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
            newFunctionButton = new JButton(SEND_EMAIL_BUTTON_LABEL);
            newFunctionButton.addActionListener(this);
        }
        return newFunctionButton;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.windowForComponent(newFunctionButton).setVisible(false);
        // This might not be the exception the user is currently looking at! 
        // Maybe it's better than nothing if it's right 80% of the time? 
        sendEmail(throwable);
    }
    
    private void sendEmail(Throwable exception) {
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
                log.warn("Error building exception suffix" , e);
            }
            mailDialogueBox.showPopupThenSendEmail();
        }
        catch (Exception ex) {
            log.warn("Error sending exception email",ex);
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), 
                    "Your message was NOT able to be sent to our support staff.  "
                    + "Please contact your support representative.");
        }
    }
    
}
