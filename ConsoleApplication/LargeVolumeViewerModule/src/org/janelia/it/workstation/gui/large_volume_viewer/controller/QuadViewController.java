/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;

/**
 * External controller of the Quad View UI.  Distances it from incoming
 * directives.
 * 
 * @author fosterl
 */
public class QuadViewController implements ViewStateListener {
    private QuadViewUi ui;
           
    public QuadViewController(QuadViewUi ui) {
        this.ui = ui;
    }
    
    @Override
    public void setCameraFocus(Vec3 focus) {
        ui.setCameraFocus(focus);
    }
    
    @Override
    public void loadColorModel(String colorModelString) {
        ui.imageColorModelFromString(colorModelString);
    }
    
    @Override
    public void pathTraceRequested(Long id) {
        ui.pathTraceRequested(id);
    }
    
    @Override
    public void centerNextParent() {
        ui.centerNextParentMicron();
    }
}
