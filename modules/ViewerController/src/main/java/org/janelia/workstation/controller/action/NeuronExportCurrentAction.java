package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.controller.NeuronManager;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.controller.model.TmSelectionState;
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
           // annotationMgr.presentError("You must select a neuron prior to performing this action.", "No neuron selected");
        }
        else {
            SwcExport export = new SwcExport();
            SwcExport.ExportParameters params = export.getExportParameters(currentNeuron.getName());
            if ( params != null ) {
               // annotationMgr.exportNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo(),
                 //   Arrays.asList(currentNeuron), params.getExportNotes());
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
