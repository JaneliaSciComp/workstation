package org.janelia.it.workstation.gui.task_workflow.actions;

/**
 *
 * @author schauderd
 */

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;

import org.janelia.it.workstation.gui.task_workflow.TaskWorkflowViewTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Task Workflow",
        id = "NextPointReviewAction"
)
@ActionRegistration(
        displayName = "Go To Next Review Point",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "A-N")
})
public class NextPointReviewAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(NextPointReviewAction.class);
    public NextPointReviewAction() {
        super("Next Review Point");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // commenting out for now since it's still in development
        // TaskWorkflowViewTopComponent.getInstance().nextTask();
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
