/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.components;

import java.util.Map;
import javax.swing.JPanel;
import org.janelia.it.workstation.cache.large_volume.GeometricNeighborhood;
import org.janelia.it.workstation.gui.large_volume_viewer.components.model.PositionalStatusModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a panel to show the states of tiles that are in specific relative
 * positions.
 *
 * @author fosterl
 */
public class PositionalStatusPanel extends JPanel {
    
    private Logger log = LoggerFactory.getLogger(PositionalStatusPanel.class);
    // Key is expected to be some sort of file-path.
    private GeometricNeighborhood neighborhood;
    
    public PositionalStatusPanel() {
    }
    
    public void set3DCacheNeighborhood(GeometricNeighborhood neighborhood) {
        log.info("New neighborhood established.");
        this.neighborhood = neighborhood;
    }
    
}
