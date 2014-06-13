/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board;

import javax.swing.JOptionPane;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.nb_action.EntityAcceptor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;

/**
 * This is to launch the whole Alignment Board suite of views.
 * 
 * @author fosterl
 */
@ServiceProvider(service = EntityAcceptor.class, path=EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH)
public class Launcher implements EntityAcceptor  {
    
    public Launcher() {
    }

    public void launch( long entityId ) {
        TopComponentGroup group = 
                WindowManager.getDefault().findTopComponentGroup(
                        "alignment_board_plugin"
                );
        if ( group != null ) {
            // This should open all members of the group.
            group.open();

            // Cause the two smaller windows to be forefront in their "modes."
            TopComponent win = WindowManager.getDefault().findTopComponent("AlignmentBoardControlsTopComponent");
            if (win.isOpened()) {
                win.requestActive();
            }

            // Make the editor one active.  This one is not allowed to be
            // arbitrarily left closed at user whim.
            win = WindowManager.getDefault().findTopComponent("AlignmentBoardTopComponent");
            if ( win != null ) {
                if ( ! win.isOpened() ) {
                    win.open();
                }
                if (win.isOpened()) {
                    win.requestActive();
                }
            }

            AlignmentBoardMgr.getInstance().getLayersPanel().openAlignmentBoard( entityId );
        }
        else {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed to open window group for plugin.");
        }

    }

    @Override
    public void acceptEntity(Entity e) {
        launch( e.getId() );
    }

    @Override
    public String getActionLabel() {
        return "  Open In Alignment Board Viewer";
    }

    @Override
    public boolean isCompatible(Entity e) {
        return e.getEntityTypeName().equals( EntityConstants.TYPE_ALIGNMENT_BOARD );
    }
    
    @Override
    public Integer getOrder() {
        return 200;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }
    
}
