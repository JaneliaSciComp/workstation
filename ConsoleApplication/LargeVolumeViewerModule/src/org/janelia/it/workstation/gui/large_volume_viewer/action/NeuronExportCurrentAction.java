package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport.ExportParameters;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
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
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        AnnotationModel annotationModel = annotationMgr.getAnnotationModel();
        TmNeuronMetadata currentNeuron = annotationModel.getCurrentNeuron();
        if (currentNeuron == null) {
            annotationMgr.presentError("You must select a neuron prior to performing this action.", "No neuron selected");
        }
        else {
            SwcExport export = new SwcExport();
            ExportParameters params = export.getExportParameters(currentNeuron.getName());
            if ( params != null ) {
                annotationMgr.exportNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo(), Arrays.asList(currentNeuron));
            }
        }
    }
}
