package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.support.SubjectComboBoxRenderer;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.security.Subject;

/**
 * this dialog lets the user request a change in ownership of the given neuron; it
 * doesn't do the work--it just provides the dialog for the user to detail the request
 */
public class ChangeNeuronOwnerDialog extends JDialog {

    private static Set<String> mouselightUsers = new HashSet<>(Arrays.asList(
            "ackermand",
            "arshadic",
            "azarr",
            "base",
            "blaker",
            "chandrashekarj",
            "dossantosb",
            "ferreirat",
            "hasanm",
            "mouselight",
            "olbrisd",
            "ramirezd2",
            "schauderd",
            "taylora",
            "weldonm",
            "winnubstj",
            "zafara"
    ));


    JComboBox subjectCombobox;
    JCheckBox mouselightOnlyCheckbox;

    private boolean success = false;

    public ChangeNeuronOwnerDialog(Frame parent, TmNeuronMetadata neuron) {
        super(parent,"Change neuron owner", true);

        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 10, 0, 10);

        // show list of users; should be able to select from list of all neurons or
        //  just users in the mouselight group
        // not sure how to get the group-only list; have to show everyone for now
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(new JLabel("Choose new owner for neuron " + neuron.getName() + ":"), constraints);

        constraints.gridy = GridBagConstraints.RELATIVE;
        subjectCombobox = new JComboBox();
        subjectCombobox.setEditable(false);
        add(subjectCombobox, constraints);

        SubjectComboBoxRenderer renderer = new SubjectComboBoxRenderer();
        subjectCombobox.setRenderer(renderer);
        subjectCombobox.setMaximumRowCount(20);

        // eventually we are going to optionally filter out non-mouselight users
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
        model.removeAllElements();

        List<Subject> subjects = new ArrayList<>();
        try {
            subjects = DomainMgr.getDomainMgr().getSubjects();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (mouselightOnlyCheckbox.isSelected()) {
            // currently no api for getting all users in a group; hardcode for now
            // note: in testing, there were occasionally resize problems when the
            //  checkbox changed--sometimes when the list got wider, the window didn't;
            //  currently, it's OK

            // playing with Java lambdas to see if it helps clarity...not sure it does
            subjects = subjects.stream()
                .filter(subject -> mouselightUsers.contains(subject.getName()))
                .collect(Collectors.toList());
        }
        for (Subject subject: subjects) {
            model.addElement(subject);
        }
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
