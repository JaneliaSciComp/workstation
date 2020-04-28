package org.janelia.workstation.controller.scripts.spatialfilter;

import org.janelia.workstation.controller.NeuronManager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Open a dialog to show neuron spatial filtering options
 */
public class NeuronFilterAction extends AbstractAction {

    private NeuronManager annotationModel;

    public NeuronFilterAction(NeuronManager annotationModel) {
        this.annotationModel = annotationModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronFilterDialog filterDialog = new NeuronFilterDialog(annotationModel);
        filterDialog.showDialog();
    }

   
}
