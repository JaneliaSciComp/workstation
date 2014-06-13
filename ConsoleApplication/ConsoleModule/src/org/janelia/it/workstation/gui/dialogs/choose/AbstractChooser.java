package org.janelia.it.workstation.gui.dialogs.choose;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.janelia.it.workstation.gui.dialogs.ModalDialog;

/**
 * A chooser for arbitrary objects. This class follows the pattern set by
 * JFileChooser.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractChooser<T> extends ModalDialog {

    public static final int ERROR_OPTION = -1;
    public static final int CANCEL_OPTION = 0;
    public static final int CHOOSE_OPTION = 1;

    private int returnValue = ERROR_OPTION;

    private final List<T> chosenElements = new ArrayList<T>();

    public AbstractChooser() {
        this("Choose", "Choose the selected elements");
    }

    public AbstractChooser(String okButtonText, String okToolTipText) {

        super();

        setPreferredSize(new Dimension(600, 800));
        setLayout(new BorderLayout());

        JButton okButton = new JButton(okButtonText);
        okButton.setToolTipText(okToolTipText);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseSelection();
                returnValue = CHOOSE_OPTION;
                setVisible(false);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnValue = CANCEL_OPTION;
                setVisible(false);
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });
    }

    protected void addChooser(JComponent component) {
        add(component, BorderLayout.CENTER);
    }

    public int showDialog(Component parent) throws HeadlessException {
        packAndShow();
        // Blocks until dialog is no longer visible, and then:
        removeAll();
        dispose();
        return returnValue;
    }

    protected void chooseSelection() {
        chosenElements.clear();
        chosenElements.addAll(choosePressed());
    }

    public List<T> getChosenElements() {
        return chosenElements;
    }

    /**
     * Override this to provide functionality that runs when the user presses
     * the "Choose" button.
     */
    protected abstract List<T> choosePressed();

}
