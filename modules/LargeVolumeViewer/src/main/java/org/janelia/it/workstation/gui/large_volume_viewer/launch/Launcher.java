package org.janelia.it.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.it.workstation.gui.passive_3d.top_component.Snapshot3dTopComponent;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ServiceProvider(service = ObjectOpenAcceptor.class, path = ObjectOpenAcceptor.LOOKUP_PATH)
public class Launcher implements ObjectOpenAcceptor  {
    
    private static final int MENU_ORDER = 300;
    
    public Launcher() {
    }

    public void launch(final Object obj) {
        
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
                    FrameworkImplProvider.handleException( ex );
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed to open window group for plugin.");
        }

        // Update "Recently Opened" history
        String strRef = Reference.createFor(domainObject).toString();
        StateMgr.getStateMgr().updateRecentlyOpenedHistory(strRef);
    }

    @Override
    public void acceptObject(Object object) {
        launch(object);
    }

    @Override
    public String getActionLabel() {
        return "  Open In Large Volume Viewer";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj != null &&  ((obj instanceof TmWorkspace) || (obj instanceof TmSample));
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
        return true;
    }
}
