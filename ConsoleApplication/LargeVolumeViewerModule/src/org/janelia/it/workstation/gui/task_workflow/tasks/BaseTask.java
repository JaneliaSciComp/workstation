package org.janelia.it.workstation.gui.task_workflow.tasks;

/**
 * this is the base class for a "task" that needs to be completed in the task workflow.
 * it's modeled after the tasks in Fly EM's Neu3 TaskProtocol
 */
abstract public class BaseTask {

    private boolean completed;


    public BaseTask() {
        setCompleted(false);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * this method may be overridden
     */


}
