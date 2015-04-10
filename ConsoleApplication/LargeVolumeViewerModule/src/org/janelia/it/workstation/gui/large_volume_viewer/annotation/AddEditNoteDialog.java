package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * allow the user to add a note to an annotation, including selecting
 * a predefined note from a list
 *
 * djo, 4/15
 */
public class AddEditNoteDialog extends JDialog {

    private boolean success = false;
    private String outputText;

    public AddEditNoteDialog(Frame parent, String inputText, TmNeuron neuron, Long annotationID) {
        super(parent, "Add note", true);


        // set up the UI
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;


        // entry field and label
        JPanel entryPanel = new JPanel();
        entryPanel.add(new JLabel("Enter note test:"));
        final JTextField entryField = new JTextField(inputText, 40);
        entryPanel.add(entryField);

        constraints.gridx = 0;
        constraints.gridy = 0;
        add(entryPanel, constraints);





        // buttons
        JButton setNoteButton = new JButton("Set note");
        setNoteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                success = true;
                outputText = entryField.getText();
                dispose();
            }
        });
        JButton deleteButton = new JButton("Delete note");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                success = true;
                outputText = "";
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                success = false;
                dispose();
            }
        });
        getRootPane().setDefaultButton(setNoteButton);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(setNoteButton);

        constraints.gridx = 0;
        constraints.gridy = 2;
        add(buttonPanel, constraints);

        pack();
        setLocationRelativeTo(parent);




    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutputText() {
        return outputText;
    }
}



