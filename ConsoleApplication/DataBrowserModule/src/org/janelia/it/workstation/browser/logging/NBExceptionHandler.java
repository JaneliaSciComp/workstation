package org.janelia.it.workstation.browser.logging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.util.UserNotificationExceptionHandler;

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
            newFunctionButton = new JButton(UserNotificationExceptionHandler.SEND_EMAIL_BUTTON_LABEL);
            newFunctionButton.addActionListener(this);
        }
        return newFunctionButton;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.windowForComponent(newFunctionButton).setVisible(false);
        // This might not be the exception the user is currently looking at! 
        // Maybe it's better than nothing if it's right 80% of the time? 
        UserNotificationExceptionHandler.sendEmail(throwable);
    }
}
