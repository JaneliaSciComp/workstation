package org.janelia.workstation.controller.spatialfilter;

import org.janelia.workstation.controller.AnnotationModel;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Open a dialog to show neuron spatial filtering options
 */
public class NeuronFilterAction extends AbstractAction {

    private AnnotationModel annotationModel;

    public NeuronFilterAction(AnnotationModel annotationModel) {
        this.annotationModel = annotationModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronFilterDialog filterDialog = new NeuronFilterDialog(annotationModel);
        filterDialog.showDialog();
    }

   
}
