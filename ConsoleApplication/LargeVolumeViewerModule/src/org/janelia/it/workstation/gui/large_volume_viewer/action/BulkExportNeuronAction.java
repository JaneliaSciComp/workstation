package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.NeuronListProvider;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport;

/**
 * this class exports selected neurons from the neuron list; it pops a dialog
 * to choose output file location
 */
public class BulkExportNeuronAction extends AbstractAction{

    private AnnotationManager annMgr;
    private AnnotationModel annModel;
    private NeuronListProvider listProvider;

    public BulkExportNeuronAction(AnnotationManager annMgr, AnnotationModel annModel, NeuronListProvider listProvider) {
        this.annMgr = annMgr;
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
            annMgr.exportNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo(),
                    listProvider.getNeuronList(), params.getExportNotes());
        }
    }
}
