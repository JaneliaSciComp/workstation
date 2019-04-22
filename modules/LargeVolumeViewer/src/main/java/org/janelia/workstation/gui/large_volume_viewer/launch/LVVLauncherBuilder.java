package org.janelia.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.workstation.gui.passive_3d.top_component.Snapshot3dTopComponent;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.common.actions.SimpleActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=1510)
public class LVVLauncherBuilder extends SimpleActionBuilder {

    @Override
    protected String getName() {
        return "Open In Large Volume Viewer";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return (obj instanceof TmWorkspace) || (obj instanceof TmSample);
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    protected void performAction(Object obj) {
        
        DomainObject domainObject = (DomainObject)obj;
        
        LargeVolumeViewerTopComponent.setRestoreStateOnOpen(false);
        
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
            final LargeVolumeViewerTopComponent win = (LargeVolumeViewerTopComponent)WindowManager.getDefault().findTopComponent(LargeVolumeViewerTopComponent.LVV_PREFERRED_ID);
            if ( win != null ) {
                if ( ! win.isOpened() ) {
                    win.open();
                }
                if (win.isOpened()) {
                    win.requestActive();
                }
                try {
                    win.openLargeVolumeViewer(domainObject);
                } catch ( Exception ex ) {
                    FrameworkAccess.handleException( ex );
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed to open window group for plugin.");
        }
    }
}
