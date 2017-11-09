package org.janelia.it.workstation.gui.task_workflow.tasks;

/**
 * Created by olbrisd on 11/8/17.
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
