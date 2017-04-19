package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.DirectedSessionAnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NextDecisionAction"
)
@ActionRegistration(
        displayName = "Next Decision (Directed Tracing)",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "O-N")
})
public class NextDecisionAction extends EditAction {

    public NextDecisionAction() {
        super("Next Decision");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        if (annotationMgr instanceof DirectedSessionAnnotationManager) {
            DirectedSessionAnnotationManager dsam = (DirectedSessionAnnotationManager)annotationMgr;
            dsam.nextDecision();
        }
    }
}
