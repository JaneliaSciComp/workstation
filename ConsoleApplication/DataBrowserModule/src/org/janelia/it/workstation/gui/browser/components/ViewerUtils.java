package org.janelia.it.workstation.gui.browser.components;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with viewers. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerUtils {

    private final static Logger log = LoggerFactory.getLogger(ViewerUtils.class);
    
    public static <T extends TopComponent> T provisionViewer(ViewerManager<T> manager, final String modeName) {

        log.debug("Provisioning viewer: {}",manager.getViewerName());
        
        T targetViewer = manager.getActiveViewer();
        if (targetViewer==null || !targetViewer.isVisible() || !targetViewer.isOpened()) {
            log.debug("Visible active viewer not found, creating...");
            
            // There is no viewer in place, so create new viewer in the appropriate area
            // TODO: this behavior should be a user preference

            try {
                targetViewer = manager.getViewerClass().newInstance();
            }
            catch (Exception e) {
                throw new IllegalStateException("Viewer instantiation failed",e);
            }
            
            log.info("Docking new instance of {} into {}",targetViewer.getName(),modeName);

            Mode mode = WindowManager.getDefault().findMode(modeName);
            mode.dockInto(targetViewer);
            // Against all reason, dockInto may cause the component to close after docking. 
            // So this open has to happen at the end. Thanks, NetBeans.
            targetViewer.open();
            
        }
        else {
            log.debug("Found active viewer");
            if (!targetViewer.isOpened()) {
                targetViewer.open();
            }
            targetViewer.requestVisible();
        }
        
        return targetViewer;
    }
}
