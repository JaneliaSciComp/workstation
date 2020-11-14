package org.janelia.workstation.common.gui.dialogs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton error dialogs
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SingletonErrorDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(SingletonErrorDialog.class);

    // How much time to keep dialog closed in the face of new events, after its been dismissed?
    private static final int DISMISS_BOUNCE_TIME_MS = 2000;

    private static final Map<String,SingletonErrorDialog> dialogMap = new HashMap<>();

    private final JLabel errorLabel;
    private final JButton okButton;
    private Long lastDismiss;

    public synchronized static SingletonErrorDialog getSingleton(String dialogIdentifier) {
        if (dialogMap.containsKey(dialogIdentifier)) {
            return dialogMap.get(dialogIdentifier);
        }
        SingletonErrorDialog dialog = new SingletonErrorDialog();
        dialogMap.put(dialogIdentifier, dialog);
        return dialog;
    }

    public SingletonErrorDialog() {

        errorLabel = new JLabel();
        errorLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        errorLabel.setBorder(new EmptyBorder(10,10,10,10));
        okButton = new JButton("Close");
        okButton.addActionListener(e -> dismiss());

        getRootPane().setDefaultButton(okButton);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);

        add(errorLabel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                okButton.requestFocus();
            }
        });
    }

    public void showDialog(String title, String errorText) {
        if (isVisible()) {
            // The singleton dialog is already showing, just bring it to the front
            log.info("Connection dialog already visible");
            toFront();
            repaint();
            return;
        }
        if (lastDismiss != null && System.currentTimeMillis() < lastDismiss + DISMISS_BOUNCE_TIME_MS) {
            log.info("Not showing error dialog for '{}' because it was very recently dismissed", title);
            return;
        }
        setTitle(title);
        errorLabel.setText(errorText);
        packAndShow();
    }

    private synchronized void dismiss() {
        lastDismiss = System.currentTimeMillis();
        setVisible(false);
    }
}
