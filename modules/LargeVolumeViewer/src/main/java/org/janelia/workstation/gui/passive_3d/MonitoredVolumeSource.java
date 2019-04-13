package org.janelia.workstation.gui.passive_3d;

import org.janelia.workstation.core.workers.IndeterminateNoteProgressMonitor;

/**
 * A special volume source which can be monitored, and will call feedback
 * routines on its monitor.
 * 
 * @author fosterl
 */
public interface MonitoredVolumeSource extends VolumeSource {
    public static final String COORDS_FORMAT = "[%3.1f,%3.1f,%3.1f]";
    void setProgressMonitor( IndeterminateNoteProgressMonitor monitor );
}
