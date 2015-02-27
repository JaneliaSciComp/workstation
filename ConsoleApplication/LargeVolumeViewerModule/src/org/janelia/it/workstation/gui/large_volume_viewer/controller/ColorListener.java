/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.awt.Color;

/**
 * Implement this to hear about changes of color.
 *
 * @author fosterl
 */
public interface ColorListener {
    void color(Color color);
}
