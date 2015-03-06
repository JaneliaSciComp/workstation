/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

/**
 * Implement this to hear about changes to the color sliders, which are
 * channel-specific.
 *
 * @author fosterl
 */
public interface ChannelColorChangeListener {
    void blackLevelChanged(Integer blackLevel);
    void gammaChanged(Double gamma);
    void whiteLevelChanged(Integer whiteLevel);
}
