package org.janelia.it.FlyWorkstation.api.entity_model.fundtype;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 3:59 PM
 */
public class TaskRequestState implements java.io.Serializable {

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