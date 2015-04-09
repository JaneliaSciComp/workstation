/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;

/**
 * Implement this to hear about a generic ID change.
 * 
 * @author fosterl
 */
public interface UpdateAnchorListener {
    void update(Anchor anchor);
}
