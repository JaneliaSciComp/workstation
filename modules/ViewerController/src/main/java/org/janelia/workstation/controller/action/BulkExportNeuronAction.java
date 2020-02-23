package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.controller.infopanel.SwcExport;
import org.janelia.workstation.controller.AnnotationModel;

/**
 * this class exports selected neurons from the neuron list; it pops a dialog
 * to choose output file location
 */
public class BulkExportNeuronAction extends AbstractAction{

    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    public BulkExportNeuronAction(AnnotationModel annModel, NeuronListProvider listProvider) {
        this.annModel = annModel;
        this.listProvider = listProvider;

        putValue(NAME, "Export neurons...");
        putValue(SHORT_DESCRIPTION, "Export all neurons in the list to swc");
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        SwcExport export = new SwcExport();
        SwcExport.ExportParameters params = export.getExportParameters(annModel.getCurrentWorkspace().getName());
        if ( params != null ) {
           // annMgr.exportNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo(),
             //       listProvider.getNeuronList(), params.getExportNotes());
        }
    }
}
