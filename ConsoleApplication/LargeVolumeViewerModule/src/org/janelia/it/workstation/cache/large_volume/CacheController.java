/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures that cache manager gets updated at the right times.
 * @author fosterl
 */
public class CacheController {
    private CacheFacade manager;
    private CacheCameraListener cameraListener;
    
    private static CacheController instance = new CacheController();
    
    public static CacheController getInstance() {
        return instance;
    }
    
    /**
     * Singleton use only.
     */
    private CacheController() {  
    }
    
    /**
     * @return the manager
     */
    public CacheFacade getManager() {
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(CacheFacade manager) {
        this.manager = manager;
        this.cameraListener = new CacheCameraListener(manager);
    }

    /**
     * Wires up the event-passage, so that the camera focus change will
     * signal the cache manager.
     * 
     * @param camera observed.
     */
    public void registerForEvents(ObservableCamera3d camera) {
        if (cameraListener != null) {
            camera.addCameraListener(cameraListener);
        }
        else {
            Logger log = LoggerFactory.getLogger(CacheController.class);
            log.warn("Attempt to register a camera for events, before the cache manager has been set.");
        }
    }

    private static class CacheCameraListener implements CameraListener {

        private CacheFacade manager;
        public CacheCameraListener( CacheFacade manager ) {
            this.manager = manager;
        }
        
        @Override
        public void viewChanged() {
            
        }

        @Override
        public void zoomChanged(Double zoom) {
            
        }

        @Override
        public void focusChanged(Vec3 focus) {
            double[] focusArr = new double[3];
            for (int i = 0; i < focusArr.length; i++) {
                focusArr[i] = focus.elementAt(i);
            }
            manager.setFocus(focusArr);
        }
        
    }
    
}
