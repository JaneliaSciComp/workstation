/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Implement this to be told when the camera-pan should be done.
 *
 * @author fosterl
 */
public interface CameraPanToListener {
    void cameraPanTo(Vec3 location);
}
