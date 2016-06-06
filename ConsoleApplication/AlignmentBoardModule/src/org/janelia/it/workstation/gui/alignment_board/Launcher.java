/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board;

import javax.swing.JOptionPane;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.browser.nb_action.DomainObjectAcceptor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;

/**
 * This is to launch the whole Alignment Board suite of views.
 * 
 * @author fosterl
 */
@ServiceProvider(service = DomainObjectAcceptor.class, path=DomainObjectAcceptor.DOMAIN_OBJECT_LOOKUP_PATH)
public class Launcher implements DomainObjectAcceptor  {
    
    private static final int MENU_ORDER = 200;
    
    public Launcher() {
    }

    public void launch( long domainObjectId ) {
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

            AlignmentBoardMgr.getInstance().getLayersPanel().openAlignmentBoard( domainObjectId );
        }
        else {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed to open window group for plugin.");
        }

    }

    @Override
    public void acceptDomainObject(DomainObject dObj) {
        launch( dObj.getId() );
    }

    @Override
    public String getActionLabel() {
        return "  Open In Alignment Board Viewer";
    }

    @Override
    public boolean isCompatible(DomainObject dObj) {
        java.util.logging.Logger.getLogger("Launcher").info(dObj.getType() + " called " + dObj.getName() + " class: " + dObj.getClass().getSimpleName());
        return dObj instanceof AlignmentBoard;
    }
    
    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }

    // May find use elsewhere...
    private String expectedTypeForClass(Class clazz) {
        String simpleName = clazz.getSimpleName();
        String[] capWords = simpleName.split("[A-Z]");
        StringBuilder rtnVal = new StringBuilder();
        for (String capWord: capWords) {
            if (rtnVal.length() > 0) {
                rtnVal.append(" ");
            }
            rtnVal.append(capWord);
        }
        return rtnVal.toString();
    }
}
