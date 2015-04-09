/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

/**
 * Implement this to hear about a neuron having been clicked.
 * 
 * @author fosterl
 */
public interface NeuronSelectedListener {
    void selectNeuron(TmNeuron neuron);
}
