/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.components.model;

import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;

/**
 * Implement this to hear about status changes.
 *
 * @author fosterl
 */
public interface PositionalStatusListener {
    void update(PositionalStatusModel model);
}
