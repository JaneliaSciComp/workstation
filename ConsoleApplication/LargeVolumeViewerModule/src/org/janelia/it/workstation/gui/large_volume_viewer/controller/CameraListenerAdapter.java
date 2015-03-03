/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.geom.Vec3;

/**
 * Extend this with a listener that does not need to hear all information.
 * Convenience class only.
 * @author fosterl
 */
public class CameraListenerAdapter implements CameraListener {

    @Override
    public void viewChanged() {}

    @Override
    public void zoomChanged(Double zoom) {}

    @Override
    public void focusChanged(Vec3 focus) {}

}
