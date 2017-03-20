package org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata;

import java.util.Date;
import java.util.List;

public class SataDecision {

    private String id;
    private String sessionId;
    private Date orderDate;
    private Date fillDate;
    private SataFocalPoint viewingFocus;
    private List<SataBranch> branches;
    private List<SataConnectivity> choices;

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

    public SataFocalPoint getViewingFocus() {
        return viewingFocus;
    }

    public void setViewingFocus(SataFocalPoint viewingFocus) {
        this.viewingFocus = viewingFocus;
    }

    public List<SataBranch> getBranches() {
        return branches;
    }

    public void setBranches(List<SataBranch> branches) {
        this.branches = branches;
    }

    public List<SataConnectivity> getChoices() {
        return choices;
    }

    public void setChoices(List<SataConnectivity> choices) {
        this.choices = choices;
    }

}