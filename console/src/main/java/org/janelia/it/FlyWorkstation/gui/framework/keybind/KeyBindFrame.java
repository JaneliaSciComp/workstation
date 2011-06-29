/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/16/11
 * Time: 9:20 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.keybind;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A dialog for setting a key binding to an action. THe dialog should be shown with showForAction.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class KeyBindFrame extends JDialog implements ActionListener {

    private static final String CLICKED_OK = "clicked_ok";
    private static final String CLICKED_CANCEL = "clicked_cancel";

    private JTextArea conflictInfoArea;
    private ShortcutTextField shortcutField;
    private Action actionToBind;

    public KeyBindFrame() {

        setTitle("Enter Keyboard Shortcut");
        setSize(200, 200);
        getContentPane().setLayout(new BorderLayout());

        setLocationRelativeTo(ConsoleApp.getMainFrame());

        shortcutField = new ShortcutTextField() {
            protected void updateCurrentKeyStrokeInfo() {

                KeyboardShortcut keyboardShortcut = getKeyboardShortcut();
                if (keyboardShortcut == null){
                    return;
                }

                Action conflictingAction = ConsoleApp.getKeyBindings().getConflict(keyboardShortcut);
                if (conflictingAction != null && !conflictingAction.equals(actionToBind)) {
                    conflictInfoArea.setForeground(Color.red);
                    conflictInfoArea.setText("Will replace key binding for '"+conflictingAction.getName()+"'");
                }
                else {
                    conflictInfoArea.setForeground(Color.black);
                    conflictInfoArea.setText("No conflicts");
                }
            }
        };
        shortcutField.setColumns(10);

        JPanel inputPane = new JPanel(new BorderLayout());
        inputPane.add(shortcutField, BorderLayout.CENTER);
        inputPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Key Stroke"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        add(inputPane, BorderLayout.NORTH);

        conflictInfoArea = new JTextArea(5, 20);
        conflictInfoArea.setEditable(false);
        conflictInfoArea.setLineWrap(true);
        conflictInfoArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(conflictInfoArea);

        JPanel conflictPane = new JPanel(new BorderLayout());
        conflictPane.add(scrollPane, BorderLayout.CENTER);
        conflictPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Conflicts"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        add(conflictPane, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.setActionCommand(CLICKED_OK);
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CLICKED_CANCEL);
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
                shortcutField.requestFocusInWindow();
            }
        });

        setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (CLICKED_OK.equals(cmd)) {
            if (actionToBind == null) {
                throw new IllegalStateException("No action to bind");
            }

            KeyboardShortcut keyboardShortcut = getKeyboardShortcut();
            ConsoleApp.getKeyBindings().setBinding(keyboardShortcut, actionToBind);
            setVisible(false);
        }
        else if (CLICKED_CANCEL.equals(cmd)) {
            setVisible(false);
        }
    }

    public void showForAction(Action action) {
        setActionToBind(action);
        setVisible(true);
    }

    public void setActionToBind(Action action) {
        this.actionToBind = action;
        KeyboardShortcut shortcut = ConsoleApp.getKeyBindings().getBinding(action);
        if (shortcut != null) {
            shortcutField.setKeyStroke(shortcut.getFirstKeyStroke());
        }
        else {
            shortcutField.setKeyStroke(null);
        }
        conflictInfoArea.setText(null);
    }

    public KeyboardShortcut getKeyboardShortcut() {
      KeyStroke firstStroke = shortcutField.getKeyStroke();
      if (firstStroke == null) {
        return null;
      }
      return new KeyboardShortcut(firstStroke);
    }

}
