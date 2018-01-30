package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

/**
 * this dialog lets the user request a change in ownership of the given neuron; it
 * doesn't do the work--it just provides the dialog for the user to detail the request
 */
public class ChangeNeuronOwnerDialog extends JDialog {

    private boolean success = false;
    private String newOwner = "";

    // this is the username for neurons that don't belong to anyone
    //  (duplicated in WorkspaceNeuronList; not sure where it should go)
    private static final String COMMON_USER = "mouselight_common";


    public ChangeNeuronOwnerDialog(Frame parent, TmNeuronMetadata neuron) {
        super(parent,"Change neuron owner", true);

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // temp
        // in the short term, we're refusing all ownership change requests
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(new JLabel("You aren't currently allowed to change ownership of neurons.  Coming soon!"), constraints);



        // it's three dialogs in one!
        String owner = neuron.getOwnerName();
        String username = AccessManager.getAccessManager().getActualSubject().getName();
        if (owner.equals(username)) {
            // if the user is the owner, they can grant ownership to someone else
            // allow choose from all users or from only Mouselight users

            constraints.gridx = 0;
            constraints.gridy = 0;
            add(new JLabel("You aren't currently allowed to change ownership of neurons.  Coming soon!"), constraints);



        } else if (owner.equals(COMMON_USER)) {
            // if the system owns the neuron, the user can have it; we expect usually this will
            //  happen transparently during other operations, but we need to handle it here
            //  as well, as quickly as possible; it's probably going to be just a "request
            //  granted" acknowledgment dialog
            constraints.gridx = 0;
            constraints.gridy = 0;
            add(new JLabel("System will change owner of neuron " + neuron.getName() + " from " + owner +
                    " to " + username), constraints);
            newOwner = username;
        } else {
            // if someone else owns the neuron (not the system), this will also be a simple
            //  "acknowlege request sent" dialog

            constraints.gridx = 0;
            constraints.gridy = 0;
            add(new JLabel("Request changing owner of neuron " + neuron.getName() + " from " + owner +
                " to " + username), constraints);
            newOwner = username;
        }

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without changing owner");
        cancelButton.addActionListener(e -> onCancel());

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Request owner change");
        okButton.addActionListener(e -> onOK());

        getRootPane().setDefaultButton(cancelButton);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        constraints.gridy = 1;
        add(buttonPane, constraints);

        pack();
        setLocationRelativeTo(parent);

        // hook up escape key
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

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

    public String getNewOwner() {
        return newOwner;
    }

}
