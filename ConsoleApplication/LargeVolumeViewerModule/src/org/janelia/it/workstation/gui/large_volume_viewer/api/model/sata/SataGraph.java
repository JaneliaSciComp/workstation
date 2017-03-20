package org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata;

public class SataGraph {
    
    private String id;
    private String samplePath;
    private SataGraphStatus status;

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

    public SataGraphStatus getStatus() {
        return status;
    }

    public void setStatus(SataGraphStatus status) {
        this.status = status;
    }
    
}
