package org.janelia.workstation.controller.task_workflow;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author schauderd
 */


public class ReviewGroup {
    private List<ReviewPoint> pointList;
    private String name;
    private boolean reviewed;
    
    ReviewGroup() {
        pointList = new ArrayList<>();
    }
    /**
     * @return the pointList
     */
    public List<ReviewPoint> getPointList() {
        return pointList;
    }

    /**
     * @param pointList the pointList to set
     */
    public void setPointList(List<ReviewPoint> pointList) {
        this.pointList = pointList;
    }    

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the reviewed
     */
    public boolean isReviewed() {
        return reviewed;
    }

    /**
     * @param reviewed the reviewed to set
     */
    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }
}
