package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.workers.BackgroundWorker;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "NeuronExportAllAction"
)
@ActionRegistration(
        displayName = "Export all neurons to SWC file",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "O-E")
})
public class NeuronExportAllAction extends AbstractAction {

    public NeuronExportAllAction() {
        super("Export SWC file...");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager annotationModel = NeuronManager.getInstance();
        if (TmModelManager.getInstance().getCurrentWorkspace() == null) {
            return;
        }

        SwcExport export = new SwcExport();
        SwcExport.ExportParameters params = export.getExportParameters(TmModelManager.getInstance().getCurrentWorkspace().getName());
        if ( params != null ) {

            //check for annotations
            int nannotations = 0;
            for (TmNeuronMetadata neuron : annotationModel.getNeuronList()) {
                nannotations += neuron.getGeoAnnotationMap().size();
            }
            if (nannotations == 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "There are no annotations in any neurons!",
                        "No annotations",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            BackgroundWorker saver = new BackgroundWorker() {
                @Override
                public String getName() {
                    return "Exporting SWC File";
                }

                @Override
                protected void doStuff() throws Exception {
                    annotationModel.exportSWCData(params.getSelectedFile(), params.getDownsampleModulo(),
                            annotationModel.getNeuronList(), params.getExportNotes(),this);
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
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        return true;
        ///return annotationMgr.getCurrentWorkspace()!=null;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
