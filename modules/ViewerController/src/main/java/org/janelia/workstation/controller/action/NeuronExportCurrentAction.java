package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;
import java.util.Arrays;

import javax.swing.*;

import org.janelia.workstation.controller.NeuronManager;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.common.gui.support.DesktopApi;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronExportCurrentAction"
)
@ActionRegistration(
        displayName = "Export current neuron to SWC file",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-E")
})
public class NeuronExportCurrentAction extends AbstractAction {

    public NeuronExportCurrentAction() {
        super("Export SWC file...");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        NeuronManager annotationModel = NeuronManager.getInstance();
        TmNeuronMetadata currentNeuron = TmSelectionState.getInstance().getCurrentNeuron();
        if (currentNeuron == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "You must select a neuron prior to performing this action.",
                    "No neuron selected",
                    JOptionPane.ERROR_MESSAGE);
        }
        else {
            SwcExport export = new SwcExport();
            SwcExport.ExportParameters params = export.getExportParameters(currentNeuron.getName());
            if ( params != null ) {
                if (currentNeuron.getGeoAnnotationMap().size() == 0) {
                    JOptionPane.showMessageDialog(
                            null,
                            "The selected neuron has no annotations!",
                            "No annotations",
                            JOptionPane.ERROR_MESSAGE);
                }

                BackgroundWorker saver = new BackgroundWorker() {
                    @Override
                    public String getName() {
                        return "Exporting SWC File";
                    }

                    @Override
                    protected void doStuff() throws Exception {
                        annotationModel.exportSWCData(params.getSelectedFile(), params.getDownsampleModulo(),
                                Arrays.asList(currentNeuron), params.getExportNotes(),this);
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

    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
      //  return LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().getCurrentWorkspace()!=null;
        return true;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
