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

    public static <T extends TopComponent> T getViewer(ViewerManager<T> manager) {

        log.info("Getting viewer: {}",manager.getViewerName());
        
        T targetViewer = manager.getActiveViewer();
        if (targetViewer!=null) {
            if (!targetViewer.isOpened()) {
                targetViewer.open();
            }
            targetViewer.requestVisible();
        }
        
        return targetViewer;
    }

    public static <T extends TopComponent> T createNewViewer(ViewerManager<T> manager, final String modeName) {

        log.info("Creating viewer: {}",manager.getViewerName());

        T viewer;
        try {
            viewer = manager.getViewerClass().newInstance();
        }
        catch (Exception e) {
            throw new IllegalStateException("Viewer instantiation failed",e);
        }
        
        log.debug("Docking new instance of {} into {}",viewer.getName(),modeName);

        Mode mode = WindowManager.getDefault().findMode(modeName);
        if (mode!=null) {
            mode.dockInto(viewer);
        }
        else {
            log.warn("No such mode found: "+modeName);
        }
        // Against all reason, dockInto may cause the component to close after docking. 
        // So, unintuitively, this open() has to happen at the end. Thanks, NetBeans.
        viewer.open();
        
        return viewer;
    }

    public static <T extends TopComponent> T getViewer(ViewerManager<T> manager, final String modeName) {

        T targetViewer = manager.getActiveViewer();
        if (targetViewer==null || !targetViewer.isVisible() || !targetViewer.isOpened()) {
            return null;
        }
        else {
            // TODO: this should probably also check to make sure the viewer is in the correct mode
            log.info("Getting viewer: {}",manager.getViewerName());
            return targetViewer;
        }
    }
    
    public static <T extends TopComponent> T provisionViewer(ViewerManager<T> manager, final String modeName) {

        log.info("Provisioning viewer: {}",manager.getViewerName());
        
        T targetViewer = manager.getActiveViewer();
        if (targetViewer==null) {
            log.info("No active viewer, looking up TC by name: {}",manager.getViewerClass().getSimpleName());
            targetViewer = (T)WindowManager.getDefault().findTopComponent(manager.getViewerClass().getSimpleName());
            if (targetViewer!=null) {
                log.info("Found TC, activating");
                manager.activate(targetViewer);
            }
        }

        if (targetViewer==null) {
            log.info("Active viewer not found, creating...");
            targetViewer = createNewViewer(manager, modeName);
        }
        else {
            log.info("Found active viewer");
            if (!targetViewer.isOpened()) {
                targetViewer.open();
            }
            if (!targetViewer.isVisible()) {
                log.info("Viewer is not visible, making active");
                targetViewer.requestActive();
            }
        }

        return targetViewer;
    }
}
