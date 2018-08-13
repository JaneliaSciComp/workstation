package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.model.security.Subject;

/**
 * this dialog lets the user request a change in ownership of the given neuron; it
 * doesn't do the work--it just provides the dialog for the user to detail the request
 */
public class ChangeNeuronOwnerDialog extends JDialog {

    JComboBox subjectCombobox;
    JCheckBox mouselightOnlyCheckbox;

    private boolean success = false;

    public ChangeNeuronOwnerDialog(Frame parent) {
        super(parent,"Change neuron owner", true);

        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 10, 0, 10);

        // show list of users; should be able to select from list of all neurons or
        //  just users in the mouselight group
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(new JLabel("Choose new owner for neuron:"), constraints);

        constraints.gridy = GridBagConstraints.RELATIVE;
        subjectCombobox = new JComboBox();
        subjectCombobox.setEditable(false);
        add(subjectCombobox, constraints);

        SubjectComboBoxRenderer renderer = new SubjectComboBoxRenderer();
        subjectCombobox.setRenderer(renderer);
        subjectCombobox.setMaximumRowCount(20);

        mouselightOnlyCheckbox = new JCheckBox("Only show mouselight users");
        mouselightOnlyCheckbox.setSelected(true);
        mouselightOnlyCheckbox.addActionListener(e -> updateList());
        add(mouselightOnlyCheckbox, constraints);

        updateList();

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without changing owner");
        cancelButton.addActionListener(e -> onCancel());

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Request owner change");
        okButton.addActionListener(e -> onOK());

        getRootPane().setDefaultButton(cancelButton);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        constraints.insets = new Insets(10, 10, 10, 10);
        add(buttonPane, constraints);

        pack();
        setLocationRelativeTo(parent);

        // hook up escape key
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

    }

    private void updateList() {
        DefaultComboBoxModel model = (DefaultComboBoxModel) subjectCombobox.getModel();
        CommonDialogItems.updateOwnerList(model, mouselightOnlyCheckbox.isSelected());
    }

    private void onCancel() {
        success = false;
        dispose();
    }

    private void onOK() {
        success = true;
        dispose();
    }

    public boolean isSuccess() {
        return success;
    }

    public Subject getNewOwnerKey() {
        Subject subject = (Subject) subjectCombobox.getSelectedItem();
        return subject;
    }

}
