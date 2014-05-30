package org.janelia.it.workstation.gui.dataview;

import org.janelia.it.workstation.nb_action.EntityAcceptor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ServiceProvider(service = EntityAcceptor.class, path=EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH)
public class Launcher implements EntityAcceptor  {
    
    public Launcher() {
    }

    public void launch( long entityId ) {
        
        DataViewerTopComponent win = (DataViewerTopComponent)WindowManager.getDefault().findTopComponent("DataViewerTopComponent");
        if ( win != null ) {
            if ( ! win.isOpened() ) {
                win.open();
            }
            if (win.isOpened()) {
                win.requestActive();
            }
            win.openDataViewer(entityId);
        }
    }

    @Override
    public void acceptEntity(Entity e) {
        launch(e.getId());
    }

    @Override
    public String getActionLabel() {
        return "  Open In Data Viewer";
    }

    @Override
    public boolean isCompatible(Entity e) {
        // Only administrators can use the data viewer
        return SessionMgr.authenticatedSubjectIsInGroup(Group.ADMIN_GROUP_NAME) || SessionMgr.currentUserIsInGroup(Group.ADMIN_GROUP_NAME);
    }

    @Override
    public Integer getOrder() {
        return 100;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return true;
    }
}
