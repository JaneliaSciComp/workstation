package org.janelia.it.workstation.api.entity_model.access;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:03 PM
 */
public abstract class TaskRequestStatusObserverAdapter implements TaskRequestStatusObserver {

    protected TaskRequestStatusObserverAdapter() {
    }//Constructor Protected to force subclassing

    public void stateChanged(org.janelia.it.workstation.api.entity_model.fundtype.TaskRequestStatus taskRequestStatus, org.janelia.it.workstation.api.entity_model.fundtype.TaskRequestState newState) {
    }

    public void loadedPercentageChanged(org.janelia.it.workstation.api.entity_model.fundtype.TaskRequestStatus taskRequestStatus, int newPercent) {
    }

    public void notifiedPercentageChanged(org.janelia.it.workstation.api.entity_model.fundtype.TaskRequestStatus taskRequestStatus, int newPercent) {
    }
//  public void alignedPercentageChanged(TaskRequestStatus taskRequestStatus,  int newPercent){}
}