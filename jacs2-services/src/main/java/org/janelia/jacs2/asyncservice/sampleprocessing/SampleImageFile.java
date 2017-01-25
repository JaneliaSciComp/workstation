package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.apache.commons.lang3.StringUtils;

public class SampleImageFile {
    private Number id;
    private String archiveFilePath;
    private String workingFilePath;
    private String chanSpec;
    private String colorSpec;
    private String divSpec;
    private Integer laser;
    private Integer gain;
    private String area;
    private String objective;

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
    }

    public String getArchiveFilePath() {
        return archiveFilePath;
    }

    public void setArchiveFilePath(String archiveFilePath) {
        this.archiveFilePath = archiveFilePath;
    }

    public String getWorkingFilePath() {
        return workingFilePath;
    }

    public void setWorkingFilePath(String workingFilePath) {
        this.workingFilePath = workingFilePath;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public boolean isChanSpecDefined() {
        return StringUtils.isNotBlank(chanSpec);
    }

    public String getColorSpec() {
        return colorSpec;
    }

    public void setColorSpec(String colorSpec) {
        this.colorSpec = colorSpec;
    }

    public String getDivSpec() {
        return divSpec;
    }

    public void setDivSpec(String divSpec) {
        this.divSpec = divSpec;
    }

    public Integer getLaser() {
        return laser;
    }

    public void setLaser(Integer laser) {
        this.laser = laser;
    }

    public Integer getGain() {
        return gain;
    }

    public void setGain(Integer gain) {
        this.gain = gain;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
}
