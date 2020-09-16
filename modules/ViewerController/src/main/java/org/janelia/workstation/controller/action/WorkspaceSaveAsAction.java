package org.janelia.workstation.controller.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Horta",
        id = "WorkspaceSaveAsAction"
)
@ActionRegistration(
        displayName = "Save a copy of the current workspace",
        lazy = true
)
public class WorkspaceSaveAsAction extends AbstractAction {

    public WorkspaceSaveAsAction() {
        super("Save as...");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        //annotationMgr.saveWorkspaceCopy();
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        //AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        //if (annotationMgr==null) return false;
        //return annotationMgr.getCurrentWorkspace()!=null;
        return true;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
