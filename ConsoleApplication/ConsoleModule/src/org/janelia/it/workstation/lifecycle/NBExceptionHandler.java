package org.janelia.it.workstation.lifecycle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler;

/**
 * Override NetBeans' exception handling to tie into the workstation's error handler.
 *
 * When the application starts up, this exception handler must be registered like this:
 *   java.util.logging.Logger.getLogger("").addHandler(new NBExceptionHandler());
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NBExceptionHandler extends Handler implements Callable<JButton>, ActionListener {

    private Throwable throwable;
    private JButton newFunctionButton;

    @Override
    public void publish(LogRecord record) {
        this.throwable = record.getThrown();
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
        UserNotificationExceptionHandler.sendEmail(throwable);
    }
}
