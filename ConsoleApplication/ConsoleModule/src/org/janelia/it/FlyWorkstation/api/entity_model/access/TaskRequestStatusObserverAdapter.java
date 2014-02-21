package org.janelia.it.FlyWorkstation.api.entity_model.access;

import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestStatus;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:03 PM
 */
public abstract class TaskRequestStatusObserverAdapter implements TaskRequestStatusObserver {

    protected TaskRequestStatusObserverAdapter() {
    }//Constructor Protected to force subclassing

    public void stateChanged(TaskRequestStatus taskRequestStatus, TaskRequestState newState) {
    }

    public void loadedPercentageChanged(TaskRequestStatus taskRequestStatus, int newPercent) {
    }

    public void notifiedPercentageChanged(TaskRequestStatus taskRequestStatus, int newPercent) {
    }
//  public void alignedPercentageChanged(TaskRequestStatus taskRequestStatus,  int newPercent){}
}