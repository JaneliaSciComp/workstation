package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.controller.infopanel.SwcExport;
import org.janelia.workstation.controller.AnnotationModel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
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
        AnnotationModel annotationModel = AnnotationModel.getInstance();
        if (annotationModel.getCurrentWorkspace()==null) return;
        SwcExport export = new SwcExport();
        SwcExport.ExportParameters params = export.getExportParameters(annotationModel.getCurrentWorkspace().getName());
        if ( params != null ) {
            //annotationMgr.exportNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo(),
              //  annotationModel.getNeuronList(), params.getExportNotes());
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
