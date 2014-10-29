package org.janelia.it.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;
import org.janelia.it.workstation.nb_action.EntityAcceptor;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic;
import org.janelia.it.workstation.gui.passive_3d.top_component.Snapshot3dTopComponent;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ServiceProvider(service = EntityAcceptor.class, path=EntityAcceptor.PERSPECTIVE_CHANGE_LOOKUP_PATH)
public class Launcher implements EntityAcceptor  {
    
    private static final int MENU_ORDER = 300;
    
    public Launcher() {
    }

    public void launch( final long entityId ) {
        TopComponentGroup group = 
                WindowManager.getDefault().findTopComponentGroup(
                        "large_volume_viewer_plugin"
                );
        
        if ( group != null ) {
            // This should open all members of the group.
            group.open();

            // Cause the smaller window to be forefront in its "mode."
            TopComponent win3d = WindowManager.getDefault().findTopComponent(Snapshot3dTopComponent.SNAPSHOT3D_TOP_COMPONENT_PREFERRED_ID);
            if (win3d.isOpened()) {
                win3d.requestActive();
            }

            // Make the editor one active.  This one is not allowed to be
            // arbitrarily left closed at user whim.
            final LargeVolumeViewerTopComponent win = (LargeVolumeViewerTopComponent)WindowManager.getDefault().findTopComponent(LargeVolumeViewerTopComponentDynamic.LVV_PREFERRED_ID);
            if ( win != null ) {
                if ( ! win.isOpened() ) {
                    win.open();
                }
                if (win.isOpened()) {
                    win.requestActive();
                }
                try {
                    win.openLargeVolumeViewer(entityId);
                } catch ( Exception ex ) {
                    SessionMgr.getSessionMgr().handleException( ex );
                }

            }

        }
        else {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed to open window group for plugin.");
        }
    }

    @Override
    public void acceptEntity(Entity e) {
        launch(e.getId());
    }

    @Override
    public String getActionLabel() {
        return "  Open In Large Volume Viewer";
    }

    @Override
    public boolean isCompatible(Entity e) {
        return e != null &&
              (EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE.equals( e.getEntityTypeName() ) ||
               EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE.equals( e.getEntityTypeName() ) );
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
        return true;
    }
}
