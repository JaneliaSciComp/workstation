package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

/**
 * Graph DTO for communicating with the Directed Tracing Workflow Service.
 * 
 * Represents a branch graph on a Tiled Microscope Sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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
