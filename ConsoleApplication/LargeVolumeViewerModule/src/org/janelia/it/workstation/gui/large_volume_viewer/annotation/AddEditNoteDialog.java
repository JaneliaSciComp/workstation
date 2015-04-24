package org.janelia.it.workstation.gui.large_volume_viewer.annotation;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * allow the user to add a note to an annotation, including selecting
 * a predefined note from a list
 *
 * djo, 4/15
 */
public class AddEditNoteDialog extends JDialog {

    private boolean success = false;
    private String outputText;

    public AddEditNoteDialog(Frame parent, final String inputText, TmNeuron neuron, Long annotationID) {
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


        // predefined term buttons: these have special behavior
        // -- first, when pressed, they exit the dialog immediately
        // -- second, some of them may not be allowed depending on
        //      the geometry of the neuron

        JPanel predefinedPanel = new JPanel();
        for (final PredefinedNote predefNote: PredefinedNote.values()) {
            JButton predefButton = new JButton(predefNote.getNoteText());
            predefButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    success = true;
                    // merge with existing note, if there is one
                    if (inputText.length() > 0) {
                        if (!inputText.endsWith(" ")) {
                            outputText = entryField.getText() + " " + predefNote.getNoteText();
                        } else {
                            outputText = entryField.getText() + predefNote.getNoteText();
                        }
                    } else {
                        outputText = predefNote.getNoteText();
                    }
                    dispose();
                }
            });
            // if a predefined note can't be applied to the input
            //  annotation, leave the button but disable it
            if (!predefNote.isValid(neuron, annotationID)) {
                predefButton.setEnabled(false);
            }
            predefinedPanel.add(predefButton);
        }

        constraints.gridx = 0;
        constraints.gridy = 1;
        add(predefinedPanel, constraints);




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
                doCancel();
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


        // hook up actions
        getRootPane().registerKeyboardAction(escapeListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

    }

    private void doCancel() {
        success = false;
        dispose();
    }


    public boolean isSuccess() {
        return success;
    }

    public String getOutputText() {
        return outputText;
    }

    private ActionListener escapeListener = new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
            doCancel();
            }
        };

    }



