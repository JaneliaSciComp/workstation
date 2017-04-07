package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport.ExportParameters;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
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
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        if (annotationMgr.getCurrentWorkspace()==null) return;
        SwcExport export = new SwcExport();
        ExportParameters params = export.getExportParameters(annotationMgr.getCurrentWorkspace().getName());
        if ( params != null ) {
            annotationMgr.exportNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo(), annotationMgr.getNeuronList());
        }
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        if (annotationMgr==null) return false;
        return annotationMgr.getCurrentWorkspace()!=null;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
