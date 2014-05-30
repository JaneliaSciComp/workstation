package org.janelia.it.workstation.api.entity_model.fundtype;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 3:59 PM
 */
public class TaskRequestState implements Serializable {

    private String stateName;

    TaskRequestState(String stateName) {
        this.stateName = stateName;
    }

    public String toString() {
        return stateName;
    }

    public boolean equals(Object targetStatus) {
        if (targetStatus instanceof TaskRequestState) {
            if (((TaskRequestState) targetStatus).stateName.equals(this.stateName)) return true;
        }
        return false;
    }

    public int hashCode() {
        return stateName.hashCode();
    }

}