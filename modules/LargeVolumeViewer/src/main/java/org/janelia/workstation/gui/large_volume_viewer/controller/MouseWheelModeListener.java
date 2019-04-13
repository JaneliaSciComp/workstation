package org.janelia.workstation.gui.large_volume_viewer.controller;

import org.janelia.workstation.gui.large_volume_viewer.action.MouseMode;
import org.janelia.workstation.gui.large_volume_viewer.action.WheelMode;

/**
 * Implement this to hear about mode changes to either mouse or its wheel.
 * @author fosterl
 */
public interface MouseWheelModeListener {
    void setMode(MouseMode.Mode modeId);
    void setMode(WheelMode.Mode modeId);
}
