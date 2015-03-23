/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.awt.Color;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * 1 stubbed implementation of a listener, to avoid having to stub them in many
 * places.
 * 
 * @author fosterl
 */
public abstract class GlobalAnnotationAdapter implements GlobalAnnotationListener {

    @Override
    public void workspaceLoaded(TmWorkspace workspace) {}

    @Override
    public void neuronSelected(TmNeuron neuron) {}

    @Override
    public void globalAnnotationColorChanged(Color color) {}

    @Override
    public void neuronStyleChanged(TmNeuron neuron, NeuronStyle style) {}
    
}
