package org.janelia.workstation.infopanel.action;

import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;

import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.action.SwcExport;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.BackgroundWorker;

/**
 * this class exports selected neurons from the neuron list; it pops a dialog
 * to choose output file location
 */
public class BulkExportNeuronAction extends AbstractAction{

    private NeuronManager annModel;
    private NeuronListProvider listProvider;

    public BulkExportNeuronAction(NeuronManager annModel, NeuronListProvider listProvider) {
        this.annModel = annModel;
        this.listProvider = listProvider;

        putValue(NAME, "Export neurons...");
        putValue(SHORT_DESCRIPTION, "Export all neurons in the list to swc");
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        SwcExport export = new SwcExport();
        SwcExport.ExportParameters params = export.getExportParameters(TmModelManager.getInstance().getCurrentWorkspace().getName());
        if ( params != null ) {
            BackgroundWorker saver = new BackgroundWorker() {
                @Override
                public String getName() {
                    return "Exporting SWC File";
                }

                @Override
                protected void doStuff() throws Exception {
                    NeuronManager.getInstance().exportSWCData(params.getSelectedFile(), params.getDownsampleModulo(),
                            listProvider.getNeuronList(), params.getExportNotes(), this);
                }

                @Override
                public Callable<Void> getSuccessCallback() {
                    return () -> {
                        DesktopApi.browse(params.getSelectedFile().getParentFile());
                        return null;
                    };
                }
            };
            saver.executeWithEvents();
        }
    }
}
