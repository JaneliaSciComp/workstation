package org.janelia.it.FlyWorkstation.gui.framework.outline.choose;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * A chooser for arbitrary objects. This class follows the pattern set by JFileChooser.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractChooser extends JComponent implements ActionListener {

    public static final int ERROR_OPTION = -1;
    public static final int CANCEL_OPTION = 0;
    public static final int CHOOSE_OPTION = 1;

    private static final String CHOOSE_COMMAND = "choose";
    private static final String CANCEL_COMMAND = "cancel";

    private String title;
    private JDialog dialog;

    private int returnValue = ERROR_OPTION;

    public AbstractChooser(String title) {
        this.title = title;
        initializeUI();
    }

    public int showDialog(Component parent) throws HeadlessException {

        JDialog dialog = createDialog(parent, title);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });

        dialog.setVisible(true);
        // Blocks until dialog is no longer visible, and then:
        dialog.removeAll();
        dialog.dispose();
        return returnValue;
    }

    private JDialog createDialog(Component parent, String title) throws HeadlessException {

        if (parent instanceof Frame) {
            dialog = new JDialog((Frame) parent, title, true);
        }
        else {
            dialog = new JDialog((JDialog) parent, title, true);
        }
        dialog.setComponentOrientation(this.getComponentOrientation());

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);

        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations = UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                // TODO; necessary?
                dialog.getRootPane().setWindowDecorationStyle(JRootPane.QUESTION_DIALOG);
            }
        }
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        return dialog;
    }

    private void initializeUI() {

        setPreferredSize(new Dimension(600, 800));
        setLayout(new BorderLayout());

        JButton okButton = new JButton("Choose");
        okButton.setActionCommand(CHOOSE_COMMAND);
        okButton.setToolTipText("Choose the selected elements");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CANCEL_COMMAND);
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (CANCEL_COMMAND.equals(cmd)) {
            cancelSelection();
        }
        else if (CHOOSE_COMMAND.equals(cmd)) {
            chooseSelection();
        }
    }

    protected void addChooser(JComponent component) {
        add(component, BorderLayout.CENTER);
    }

    /**
     * Override this to provide functionality that runs when the user presses the "Choose" button.
     */
    protected abstract void choosePressed();

    protected void chooseSelection() {

        choosePressed();

        returnValue = CHOOSE_OPTION;
        dialog.setVisible(false);
    }

    protected void cancelSelection() {
        returnValue = CANCEL_OPTION;
        dialog.setVisible(false);
    }
}
