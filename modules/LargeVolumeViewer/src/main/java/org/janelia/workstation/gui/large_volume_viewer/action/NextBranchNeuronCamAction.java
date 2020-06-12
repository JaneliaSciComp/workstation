package org.janelia.workstation.gui.large_volume_viewer.action;

/**
 *
 * @author schauderd
 */

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.controller.task_workflow.TaskWorkflowViewTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "NextBranchNeuronCamAction"
)
@ActionRegistration(
        displayName = "Review Next Branch",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "A-9")
})
public class NextBranchNeuronCamAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(NextBranchNeuronCamAction.class);
    public NextBranchNeuronCamAction() {
        super("Review Next Branch");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        TaskWorkflowViewTopComponent taskView = TaskWorkflowViewTopComponent.getInstance();
        if (taskView!=null)
            taskView.nextBranch();
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
