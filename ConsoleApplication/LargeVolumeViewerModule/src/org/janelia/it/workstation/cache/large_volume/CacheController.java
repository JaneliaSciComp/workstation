/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import org.janelia.it.workstation.gui.large_volume_viewer.CustomNamedThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.SharedVolumeImage;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.components.PositionalStatusPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures that cache manager gets updated at the right times.
 * @author fosterl
 */
public class CacheController {
    public static final int CACHE_MGR_SLEEP_TIME = 100;
    private static final boolean POLLING = true;

    private CacheFacadeI manager;
    private CacheCameraListener cameraListener;
    private GeometricNeighborhoodBuilder neighborhoodBuilder;
    private PositionalStatusPanel posStatPanel;
    private static ExecutorService executor;
    
    private boolean inUse = true;
    
    private static final CacheController instance = new CacheController();
    private static Logger log = LoggerFactory.getLogger(CacheController.class);

    public static CacheController getInstance() {
        return instance;
    }
    
    /**
     * Singleton use only.
     */
    private CacheController() {
        // Create a queue-like thread pool.
        executor = Executors.newFixedThreadPool(1, new CustomNamedThreadFactory("CacheControllerThread"));
    }
    
    /**
     * @return the manager
     */
    public CacheFacadeI getManager() {
        awaitCacheManager();
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(CacheFacadeI manager) {
        this.manager = manager;
        this.cameraListener = new CacheCameraListener(manager);
        if (POLLING) {
            establishPollTimer();
        }
    }
    
    public void setInUse( boolean flag ) {
        this.inUse = flag;
    }
    
    public void close() {
        if (manager != null) {
            manager.close();
        }
    }

    /**
     * Wires up the event-passage, so that the camera focus change will
     * signal the cache manager.
     * 
     * @param camera observed.
     */
    public void registerForEvents(ObservableCamera3d camera, SharedVolumeImage sharedVolumeImage) {
        if (cameraListener != null) {
            // No longer direct listening.  camera.addCameraListener(cameraListener);
            cameraListener.setCamera(camera);
            cameraListener.setSharedVolumeImage(sharedVolumeImage);
        }
        else {
            log.warn("Attempt to register a camera for events, before the cache manager has been set.");
        }
    }

    /**
     * Makes neighborhood's events available to everything around this controller.
     * @param neighborhoodBuilder 
     */
    public void registerForEvents(GeometricNeighborhoodBuilder neighborhoodBuilder) {
        this.neighborhoodBuilder = neighborhoodBuilder;
        establishGeoNeighborhoodListener();
    }
    
    public void loadInProgress(File infile) {
        if (posStatPanel != null)
            posStatPanel.setLoadInProgress(infile);
        else
            log.warn("No positional status panel");
    }
    
    public void loadComplete(File infile, int timeMs) {
        if (posStatPanel != null)
            posStatPanel.setLoadComplete(infile, timeMs);
    }

    /**
     * Makes sure messages can be routed through to anno-panel and beyond.
     * @param annotationPanel 
     */
    public void registerForEvents(PositionalStatusPanel panel) {
        this.posStatPanel = panel;
        establishGeoNeighborhoodListener();
    }
    
    // Will poll periodically to see whether most recently-received
    // value matches last set value.  If not, proceed.
    private Double receivedZoom;
    private Double lastSetZoom = null;
    private Vec3 receivedFocus;
    private Vec3 lastSetFocus = null;

    public synchronized void zoomChanged(Double zoom) {
        if (cameraListener == null) {
            return;
        }
        receivedZoom = zoom;
        if (! POLLING)
            cameraListener.zoomChanged(zoom);
    }
    
    public void focusChanged(Vec3 focus) {
        if (cameraListener == null) {
            return;
        }
        receivedFocus = focus;
        if (! POLLING)
            cameraListener.focusChanged(focus);
    }
    
    private void establishGeoNeighborhoodListener() {
        if (posStatPanel != null  &&  neighborhoodBuilder != null) {
            neighborhoodBuilder.setListener(new NeighborhoodUpdater(posStatPanel));
        }
    }

    /** Ensure that this thing gets set.  */
    private CacheFacadeI awaitCacheManager() {
        if (manager != null) {
            return manager;
        }
        else if (! inUse) {
            return null;
        }
        log.trace("Awaiting cache manager.");
        boolean found = false;
        int maxRetryTime = 1000 * 60;
        while (!found) {
            try {
                Thread.sleep(CACHE_MGR_SLEEP_TIME);                
                // This loop will break in response to a manager being set,
                // or to a flag of "in-use" being set false.
                if (manager != null  ||  (!inUse)) {
                    found = true;
                }            
                else {
                    maxRetryTime -= CACHE_MGR_SLEEP_TIME;
                    if (maxRetryTime < 0) {
                        log.error("Time out awaiting cache mananger.");
                        return null;
                    }
                }
            } catch (Exception ex) {
                log.debug("Exception while awaiting cache mgr. {}",
                        ex.getMessage());
            }
        }
        log.trace("Cache manager wait is over.");
        return manager;
    }
    
    private void establishPollTimer() {
        Timer pollTimer = new Timer("CacheControllerTimer");
        TimerTask pollTask = new TimerTask() {

            @Override
            public void run() {
                if (receivedZoom != null  &&   (lastSetZoom == null  ||  !receivedZoom.equals( lastSetZoom ))) {
                    lastSetZoom = receivedZoom;
                    cameraListener.zoomChanged(lastSetZoom);
                    if (POLLING) {
                        cameraListener.focusChanged(lastSetFocus);
                    }
                }
                if (receivedFocus != null  &&  (lastSetFocus == null  ||  !receivedFocus.equals( lastSetFocus ))) {
                    lastSetFocus = receivedFocus;
                    cameraListener.focusChanged(lastSetFocus);
                }
            }
            
        };
        pollTimer.scheduleAtFixedRate(pollTask, 5, 3000);
    }

    private static class CacheCameraListener implements CameraListener {

        private final CacheFacadeI manager;
        private SharedVolumeImage sharedVolumeImage;
        private ObservableCamera3d camera;
        private final AtomicReference<Vec3> focusInWaiting = new AtomicReference<>();
        private final AtomicReference<Double> zoomInWaiting = new AtomicReference<>();
        
        public CacheCameraListener( CacheFacadeI manager ) {
            this.manager = manager;
        }
        
        public void setCamera(ObservableCamera3d camera) {
            this.camera = camera;
        }
        
        public void setSharedVolumeImage( SharedVolumeImage sharedVolumeImage ) {
            this.sharedVolumeImage = sharedVolumeImage;
        }
        
        @Override
        public void viewChanged() {            
        }

        /** Current iteration is not using any zoom except 1. */
        @Override
        public void zoomChanged(Double zoom) {
            Double oldZoom = zoomInWaiting.getAndSet(zoom);
            if (oldZoom == null) {
                Runnable r = new ZoomChanger(manager, camera, sharedVolumeImage, zoomInWaiting);
                executor.submit(r);
            }
        }

        @Override
        public void focusChanged( Vec3 focus ) {
            log.info("Focus Change {}", focus);
            // Make sure that any thread, which is waiting to set the
            // focus, uses this new focus.
            Vec3 oldFocus = focusInWaiting.getAndSet(focus);
            if (oldFocus == null) {
                // No thread waiting: create a new change-of-focus runnable, and
                // run it in a thread, behind any currently-running thread.
                // Ensure some thread is waiting in the queue for this.
                Runnable r = new FocusChanger(camera, sharedVolumeImage, manager, focusInWaiting);
                executor.submit(r);
            }
        }
        
    }

    /**
     * This task will carry out focus modification in a separate thread
     * from the event thread.
     */
    private static class FocusChanger implements Runnable {
        private ObservableCamera3d camera;
        private SharedVolumeImage sharedVolumeImage;
        private CacheFacadeI manager;
        private AtomicReference<Vec3> focus;
        
        public FocusChanger(ObservableCamera3d camera, SharedVolumeImage sharedVolumeImage, CacheFacadeI manager, AtomicReference<Vec3> focus) {
            this.camera = camera;
            this.sharedVolumeImage = sharedVolumeImage;
            this.manager = manager;
            this.focus = focus;
        }
        
        @Override
        public void run() {
            TileFormat tileFormat = sharedVolumeImage.getLoadAdapter().getTileFormat();
            Double zoom = (double) tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
            if (zoom != null) {
                manager.setCameraZoomValue(zoom);
            }

            double[] focusArr = new double[3];
            Vec3 referencedFocus = focus.get();
            focus.set(null);
            for (int i = 0; i < focusArr.length; i++) {
                focusArr[i] = referencedFocus.elementAt(i);
            }
            manager.setPixelsPerSceneUnit(1.0); //camera.getPixelsPerSceneUnit());
            manager.setFocus(focusArr);            
        }
    }
    
    private static class ZoomChanger implements Runnable {
        private CacheFacadeI manager;
        private SharedVolumeImage sharedVolumeImage;
        private ObservableCamera3d camera;
        private AtomicReference<Double> zoom;
        
        public ZoomChanger(CacheFacadeI manager, ObservableCamera3d camera, SharedVolumeImage sharedVolumeImage, AtomicReference<Double> zoom) {
            this.manager = manager;
            this.camera = camera;
            this.zoom = zoom;
        }
        
        @Override
        public void run() {
            Double referencedZoom = zoom.get();
            zoom.set(null);
            //TileFormat tileFormat = sharedVolumeImage.getLoadAdapter().getTileFormat();
            //Double zoom = (double) tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
            manager.setPixelsPerSceneUnit(1.0);//camera.getPixelsPerSceneUnit());
            if (POLLING) {
                manager.setCameraZoomValue(referencedZoom);
            }
            else {
                manager.setCameraZoom(referencedZoom);
            }
        }
        
    }
    
    private static class NeighborhoodUpdater implements GeometricNeighborhoodListener {

        private PositionalStatusPanel panel;
        public NeighborhoodUpdater(PositionalStatusPanel panel) {
            this.panel = panel;
        }
        
        @Override
        public void created(GeometricNeighborhood neighborhood) {
            panel.set3DCacheNeighborhood(neighborhood);
        }

    }
}
