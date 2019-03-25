/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.gui.large_volume_viewer.action.MouseMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.WheelMode;

/**
 * Implement this to hear about mode changes to either mouse or its wheel.
 * @author fosterl
 */
public interface MouseWheelModeListener {
    void setMode(MouseMode.Mode modeId);
    void setMode(WheelMode.Mode modeId);
}
