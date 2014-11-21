/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer;

import javax.swing.JComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

/**
 * Utilities relay to avoid direct dependencies on things like the top component.
 * 
 * @author fosterl
 */
public class ComponentUtil {
    public static JComponent getLVVMainWindow() {
        return LargeVolumeViewerTopComponent.findThisComponent();
    }
}
