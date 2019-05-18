package org.janelia.workstation.gui.large_volume_viewer.dialogs;

import org.janelia.model.security.Subject;
import org.janelia.workstation.common.gui.support.SubjectComboBoxRenderer;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.ConsoleProperties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

/**
 * this dialog lets the user request a change in ownership of the given neuron; it
 * doesn't do the work--it just provides the dialog for the user to detail the request
 */
public class ChangeNeuronOwnerDialog extends JDialog {

    public enum UserFilter {NONE, TRACERS, ACTIVE_TRACERS};
    UserFilter userFilter;

    JComboBox subjectCombobox;
    JCheckBox filterUserListCheckbox;

    private boolean success = false;

    public ChangeNeuronOwnerDialog(Frame parent) {
        super(parent,"Change neuron owner", true);

        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 10, 0, 10);

        // show list of users; should be able to select from list of all users or
        //  just active users from a specific group:
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


        // this check box's presence and text depends on whether particular
        //  groups exist; the group names are stored as properties
        Subject tracersSubject = null;
        Subject activeTracersSubject = null;
        try {
            String activeTracersGroup = ConsoleProperties.getInstance().getProperty("console.LVVHorta.activetracersgroup").trim();
            String tracersGroup = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
            tracersSubject = DomainMgr.getDomainMgr().getSubjectFacade().getSubjectByNameOrKey(tracersGroup);
            activeTracersSubject = DomainMgr.getDomainMgr().getSubjectFacade().getSubjectByNameOrKey(activeTracersGroup);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        String message;
        if (activeTracersSubject != null) {
            userFilter = UserFilter.ACTIVE_TRACERS;
            message = "Only show active tracers";
        } else if (tracersSubject != null) {
            userFilter = UserFilter.TRACERS;
            message = "Only show neuron tracers";
        } else {
            userFilter = UserFilter.NONE;
            message = "";
        }

        filterUserListCheckbox = new JCheckBox();
        if (userFilter != UserFilter.NONE) {
            filterUserListCheckbox.setText(message);
            filterUserListCheckbox.setSelected(true);
            filterUserListCheckbox.addActionListener(e -> updateList());
            add(filterUserListCheckbox, constraints);
        } else {
            // couldn't find groups, so leave the checkbox false and
            //  don't even show it (don't add to the layout)
            filterUserListCheckbox.setSelected(false);
        }

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
        if (filterUserListCheckbox.isSelected()) {
            CommonDialogItems.updateOwnerList(model, userFilter);
        } else {
            CommonDialogItems.updateOwnerList(model, UserFilter.NONE);
        }
        // needed to make the dialog resize properly for the new name list
        pack();
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
