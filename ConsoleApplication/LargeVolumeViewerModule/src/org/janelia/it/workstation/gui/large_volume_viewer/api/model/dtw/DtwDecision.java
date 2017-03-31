package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

import java.util.Date;
import java.util.List;

public class DtwDecision {

    private String id;
    private String sessionId;
    private Date orderDate;
    private Date fillDate;
    private DtwFocalPoint viewingFocus;
    private List<DtwBranch> branches;
    private List<DtwConnectivity> choices;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public Date getFillDate() {
        return fillDate;
    }

    public void setFillDate(Date fillDate) {
        this.fillDate = fillDate;
    }

    public DtwFocalPoint getViewingFocus() {
        return viewingFocus;
    }

    public void setViewingFocus(DtwFocalPoint viewingFocus) {
        this.viewingFocus = viewingFocus;
    }

    public List<DtwBranch> getBranches() {
        return branches;
    }

    public void setBranches(List<DtwBranch> branches) {
        this.branches = branches;
    }

    public List<DtwConnectivity> getChoices() {
        return choices;
    }

    public void setChoices(List<DtwConnectivity> choices) {
        this.choices = choices;
    }

}