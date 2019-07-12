package org.janelia.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.workstation.gui.passive_3d.top_component.Snapshot3dTopComponent;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches the Data Viewer from a context-menu.
 */
@ActionID(
        category = "Actions",
        id = "OpenInLVVAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenInLVVAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Large Volume", position = 1510, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_OpenInLVVAction=Open In Large Volume Viewer")
public class OpenInLVVAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(OpenInLVVAction.class);
    private DomainObject domainObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmSample.class)) {
            domainObject = getNodeContext().getSingleObjectOfType(TmSample.class);
            setEnabledAndVisible(true);
        }
        else if (getNodeContext().isSingleObjectOfType(TmWorkspace.class)) {
            domainObject = getNodeContext().getSingleObjectOfType(TmWorkspace.class);
            setEnabledAndVisible(true);
        }
        else {
            domainObject = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        DomainObject objectToOpen = domainObject;
        if (objectToOpen==null) {
            log.warn("Action performed with null domain object");
            return;
        }

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
            if (win != null) {
                if (!win.isOpened()) {
                    win.open();
                }
                if (win.isOpened()) {
                    win.requestActive();
                }
                try {
                    win.openLargeVolumeViewer(objectToOpen);
                }
                catch (Exception ex) {
                    FrameworkAccess.handleException( ex );
                }
            }

            FrameworkAccess.getBrowsingController().updateRecentlyOpenedHistory(Reference.createFor(objectToOpen));
        }
        else {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed to open window group for plugin.");
        }
    }
}
