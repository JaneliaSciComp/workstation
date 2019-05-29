package org.janelia.workstation.gui.large_volume_viewer.annotation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Open a dialog to show neuron spatial filtering options
 */
public class NeuronFilterAction extends AbstractAction {

    private AnnotationModel annotationModel;
    private AnnotationManager annotationManager;

    public NeuronFilterAction(AnnotationModel annotationModel, AnnotationManager annotationManager) {
        this.annotationModel = annotationModel;
        this.annotationManager = annotationManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronFilterDialog filterDialog = new NeuronFilterDialog(annotationManager, annotationModel);
        filterDialog.showDialog();
    }

   
}
