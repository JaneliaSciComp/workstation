package org.janelia.it.workstation.gui.alignment_board;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.alignment_board.AlignmentBoard;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is to launch the whole Alignment Board suite of views.
 * 
 * @author fosterl
 */
@ServiceProvider(service = ObjectOpenAcceptor.class, path=ObjectOpenAcceptor.LOOKUP_PATH)
public class Launcher implements ObjectOpenAcceptor  {
    
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    
    private static final int MENU_ORDER = 200;

    public void launch(long domainObjectId) {
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
    public void acceptObject(Object obj) {
        if (obj instanceof DomainObject) {
            
            DomainObject domainObject = (DomainObject) obj;
            launch(domainObject.getId());

            // Update "Recently Opened" history
            String strRef = Reference.createFor(domainObject).toString();
            StateMgr.getStateMgr().updateRecentlyOpenedHistory(strRef);
        }
    }

    @Override
    public String getActionLabel() {
        return "  Open In Alignment Board Viewer";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof AlignmentBoard;
    }
    
    @Override
    public boolean isEnabled(Object obj) {
        return true;
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
}
