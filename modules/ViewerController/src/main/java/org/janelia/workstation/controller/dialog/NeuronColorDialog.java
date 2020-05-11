package org.janelia.workstation.controller.dialog;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * allow the user to determine the style the annotations
 * for a neuron will be drawn with
 *
 * djo, 3/15
 */
public class NeuronColorDialog extends JDialog {

    private Color chosenColor;

    // did the user choose a style or cancel?
    private boolean success = false;


    public NeuronColorDialog(Frame parent, Color base) {

        super(parent, "Choose neuron color", true);


        // set up the UI
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // color chooser
        final JColorChooser chooser = new JColorChooser(base);
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(chooser, constraints);

        // accept/cancel buttons
        JButton acceptButton = new JButton("Accept");
        acceptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chosenColor = chooser.getColor();
                success = true;
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

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        buttonPanel.add(acceptButton);

        constraints.gridx = 0;
        constraints.gridy = 2;
        add(buttonPanel, constraints);

        pack();
        setLocationRelativeTo(parent);


    }

    public Color getChosenColor() {
        return chosenColor;
    }

    public boolean styleChosen() {
        return success;
    }

}
