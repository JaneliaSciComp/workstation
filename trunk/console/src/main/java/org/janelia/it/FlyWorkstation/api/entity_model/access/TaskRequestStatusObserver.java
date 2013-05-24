package org.janelia.it.FlyWorkstation.api.entity_model.access;

import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.TaskRequestStatus;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:02 PM
 */
public interface TaskRequestStatusObserver {

    void stateChanged(TaskRequestStatus taskRequestStatus, TaskRequestState newState);

    void loadedPercentageChanged(TaskRequestStatus taskRequestStatus, int newPercent);

    void notifiedPercentageChanged(TaskRequestStatus taskRequestStatus, int newPercent);
//   void alignedPercentageChanged(TaskRequestStatus taskRequestStatus,  int newPercent);

}