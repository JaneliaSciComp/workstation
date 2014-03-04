/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.FlyWorkstation.gui.alignment_board;

import org.janelia.it.FlyWorkstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.FlyWorkstation.nb_action.EntityAcceptor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
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
        TopComponent win = WindowManager.getDefault().findTopComponent("AlignmentBoardControlsTopComponent");
        if ( win.isOpened() ) {
            win.close();
        }
        win.open();

        win = WindowManager.getDefault().findTopComponent("AlignmentBoardTopComponent");
        if ( win.isOpened() ) {
            win.close();
        }
        win.open();
        win.requestActive();

        win = WindowManager.getDefault().findTopComponent("LayersPanelTopComponent");
        if ( win.isOpened() ) {
            win.close();
        }
        win.open();
        
        AlignmentBoardMgr.getInstance().getLayersPanel().openAlignmentBoard( entityId );

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

}
