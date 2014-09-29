/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d;

import org.janelia.it.workstation.shared.workers.IndeterminateNoteProgressMonitor;

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
