package org.janelia.it.FlyWorkstation.api.entity_model.access;

import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestStatus;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:02 PM
 */
public interface LoadRequestStatusObserver {

    void stateChanged(LoadRequestStatus loadRequestStatus, LoadRequestState newState);

    void loadedPercentageChanged(LoadRequestStatus loadRequestStatus, int newPercent);

    void notifiedPercentageChanged(LoadRequestStatus loadRequestStatus, int newPercent);
//   void alignedPercentageChanged(LoadRequestStatus loadRequestStatus,  int newPercent);

}