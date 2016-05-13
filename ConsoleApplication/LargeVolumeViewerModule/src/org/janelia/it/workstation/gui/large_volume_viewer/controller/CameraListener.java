/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Implement this to hear about camera-related events.
 * @author fosterl
 */
public interface CameraListener {
    void viewChanged();
    void zoomChanged(Double zoom);
    void focusChanged(Vec3 focus);
}
