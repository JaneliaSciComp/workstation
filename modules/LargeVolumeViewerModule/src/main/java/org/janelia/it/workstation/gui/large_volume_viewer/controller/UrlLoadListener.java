/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.net.URL;

/**
 * Implement this to hear about when URLs are loaded.
 * 
 * @author fosterl
 */
public interface UrlLoadListener {
    void loadUrl(URL url);
}
