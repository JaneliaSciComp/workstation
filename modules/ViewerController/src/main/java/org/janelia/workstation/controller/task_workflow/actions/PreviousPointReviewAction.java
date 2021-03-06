package org.janelia.workstation.controller.task_workflow.actions;

/**
 *
 * @author schauderd
 */

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Task Workflow",
        id = "PreviousPointReviewAction"
)
@ActionRegistration(
        displayName = "Go To Previous Review Point",
        lazy = true
)
public class PreviousPointReviewAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(PreviousPointReviewAction.class);
    public PreviousPointReviewAction() {
        super("Previous Review Point");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // commenting out for now since it's still in development
        // TaskWorkflowViewTopComponent.getInstance().prevTask()
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
