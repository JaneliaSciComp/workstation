package org.janelia.workstation.core.model;

import org.janelia.model.domain.enums.SplitHalfType;

public class SplitHalf {

    private String driver;
    private String flycoreId;
    private String info;
    private String line;
    private String project;
    private String robotId;
    private String subcategory;
    private SplitHalfType type;

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getFlycoreId() {
        return flycoreId;
    }

    public void setFlycoreId(String flycoreId) {
        this.flycoreId = flycoreId;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getRobotId() {
        return robotId;
    }

    public void setRobotId(String robotId) {
        this.robotId = robotId;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public SplitHalfType getType() {
        return type;
    }

    public void setType(SplitHalfType type) {
        this.type = type;
    }
}