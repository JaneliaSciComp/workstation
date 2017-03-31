package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

public class DtwGraph {
    
    private String id;
    private String samplePath;
    private DtwGraphStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSamplePath() {
        return samplePath;
    }

    public void setSamplePath(String samplePath) {
        this.samplePath = samplePath;
    }

    public DtwGraphStatus getStatus() {
        return status;
    }

    public void setStatus(DtwGraphStatus status) {
        this.status = status;
    }
    
}
