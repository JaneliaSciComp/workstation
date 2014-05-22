package org.janelia.it.workstation.api.entity_model.fundtype;

/**
 * @author Peter Davies
 */

public class TaskFilterStatus implements java.io.Serializable {

    private static final long serialVersionUID = 1;
    private TaskFilter taskFilter;
    private boolean completed;

    TaskFilterStatus(TaskFilter taskFilter) {
        this.taskFilter = taskFilter;
    }

    public boolean isCompleted() {
        return completed;
    }

    protected void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * This gets called by the TaskRequestStatus when the state changes to Complete
     */
    void requestCompleted(TaskRequest request) {
//     if (request.isBinRequest() || request.isRangeRequest()) throw
//        new IllegalStateException("The request made with the filter is either a"+
//          " range or binned request, but the TaskFilterStatus is not the right class for these." );
        completed = true;
    }


}