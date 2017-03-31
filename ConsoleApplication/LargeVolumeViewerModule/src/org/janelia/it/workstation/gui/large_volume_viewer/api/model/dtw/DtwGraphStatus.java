package org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw;

/**
 * Graph status DTO for communicating with the Directed Tracing Workflow Service.
 * 
 * Represents the current status of a branch graph in the DTW Service.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum DtwGraphStatus {
    Loading,
    Ready,
    Updating,
    Error,
    Unknown
}
