package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;

import java.awt.*;


// workstation imports

import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.Slot1;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected neuron: name, type, etc.
 *
 * djo, 6/13
 */
public class NeuronInfoPanel extends JPanel 
{

    JLabel neuronNameLabel;


    public Slot1<TmNeuron> updateNeuronSlot = new Slot1<TmNeuron>() {
        @Override
        public void execute(TmNeuron neuron) {
            updateNeuron(neuron);
        }
    };



    public NeuronInfoPanel() {
        setupUI();
    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
        // neuron information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Neuron information panel"));

        neuronNameLabel = new JLabel("");
        add(neuronNameLabel);


        updateNeuron(null);
    }


    public void updateNeuron(TmNeuron neuron) {
        if (neuron == null) {
            neuronNameLabel.setText("(no neuron)");
        } else {
            neuronNameLabel.setText(neuron.getName());

            // try to extract some info and print it, for testing:
            System.out.println("loaded neuron " + neuron.getName());
            TmGeoAnnotation rootAnn = neuron.getRootAnnotation();
            if (rootAnn == null) {
                System.out.println("neuron has no root annotation");
            } else {
                double x = rootAnn.getX();
                double y = rootAnn.getY();
                double z = rootAnn.getZ();
                int nChildren = rootAnn.getChildren().size();
                System.out.println("root annotation at " + x + ", " + y + ", " + z);
                System.out.println("root annotation has " + nChildren + " children");
            }
        }
    }

}