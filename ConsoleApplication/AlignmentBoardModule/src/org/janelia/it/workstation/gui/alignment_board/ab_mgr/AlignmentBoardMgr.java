/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board.ab_mgr;

import org.janelia.it.workstation.gui.alignment_board_viewer.LayersPanel;

/**
 * This is a service provider for the Layers Panel.
 * 
 * @author fosterl
 */
public class AlignmentBoardMgr {
    private static AlignmentBoardMgr instance = new AlignmentBoardMgr();
    private LayersPanel layersPanel = new LayersPanel();
    private AlignmentBoardMgr() {}
    public static AlignmentBoardMgr getInstance() {
        return instance;
    }
    
    /**
     * This is the single source for the layers panel.
     * 
     * @return one and only
     */
    public LayersPanel getLayersPanel() {
        return layersPanel;
    }
}
