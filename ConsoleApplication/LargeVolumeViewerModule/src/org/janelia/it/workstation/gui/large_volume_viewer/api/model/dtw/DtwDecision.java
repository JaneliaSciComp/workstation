package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Decision DTO for communicating with the Directed Tracing Workflow Service.
 * 
 * Represents a decision which was requested by the DTW Service, and which the user 
 * updates with their selected choice.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DtwDecision {

    private String id;
    private String sessionId;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private Date orderDate;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private Date fillDate;
    private DtwFocalPoint viewingFocus;
    private List<DtwBranch> branches;
    private List<DtwConnectivity> choices;
    private Integer choiceIndex;

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

    public Integer getChoiceIndex() {
        return choiceIndex;
    }

    public void setChoiceIndex(Integer choiceIndex) {
        this.choiceIndex = choiceIndex;
    }

}