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
    public static final String ALIGNMENT_BOARD_TOP_COMPONENT_ID = "AlignmentBoardTopComponent";
    public static final String ALIGNMENT_BOARD_CTRLS_TOP_COMPONENT_ID = "AlignmentBoardControlsTopComponent";

    private static final AlignmentBoardMgr instance = new AlignmentBoardMgr();
    private final LayersPanel layersPanel = new LayersPanel();
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
