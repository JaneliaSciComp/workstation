package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.annotation;


// std lib imports

import javax.swing.*;

import java.awt.*;


// workstation imports

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

/**
 * this panel shows info on the selected neuron: name, type, etc.
 *
 * djo, 6/13
 */
public class NeuriteTreePanel extends JPanel 
{



    public NeuriteTreePanel() {
        setupUI();
    }

    public void clear() {

    }

    private void setupUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
        // neuron information; show name, whatever attributes
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(new JLabel("Neurite tree panel"));


        clear();
    }


    public void update(TmNeuron neuron) {

    }

}