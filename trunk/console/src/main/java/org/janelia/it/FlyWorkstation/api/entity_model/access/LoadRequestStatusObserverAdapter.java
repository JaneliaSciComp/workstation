package org.janelia.it.FlyWorkstation.api.entity_model.access;

import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestState;
import org.janelia.it.FlyWorkstation.api.entity_model.fundtype.LoadRequestStatus;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:03 PM
 */
public abstract class LoadRequestStatusObserverAdapter implements LoadRequestStatusObserver {

    protected LoadRequestStatusObserverAdapter() {
    }//Constructor Protected to force subclassing

    public void stateChanged(LoadRequestStatus loadRequestStatus, LoadRequestState newState) {
    }

    public void loadedPercentageChanged(LoadRequestStatus loadRequestStatus, int newPercent) {
    }

    public void notifiedPercentageChanged(LoadRequestStatus loadRequestStatus, int newPercent) {
    }
//  public void alignedPercentageChanged(LoadRequestStatus loadRequestStatus,  int newPercent){}
}