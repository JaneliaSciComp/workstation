/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.workstation.gui.large_volume_viewer.RepaintListener;
import org.janelia.it.workstation.gui.large_volume_viewer.UpdateAnchorListener;

/**
 * Relay for updating state listeners.  This removes the verbiage from the
 * class under study.
 * 
 * @author fosterl
 */
public class SkeletonActorStateUpdater {
    private List<RepaintListener> updateListeners = new ArrayList<>();
    private List<UpdateAnchorListener> updateAnchorListeners = new ArrayList<>();
    
    public void addListener( RepaintListener listener ) {
        updateListeners.add(listener);
    }
    
    public void removeListener( RepaintListener listener ) {
        updateListeners.remove(listener);
    }
    
    public void addListener( UpdateAnchorListener listener ) {
        updateAnchorListeners.add(listener);
    }
    
    public void removeListener( UpdateAnchorListener listener ) {
        updateAnchorListeners.remove(listener);
    }
    
    public void update() {
        for (RepaintListener l: updateListeners) {
            l.repaint();
        }
    }
    
    public void update(Anchor anchor) {
        for (UpdateAnchorListener l: updateAnchorListeners ) {
            l.update(anchor);
        }
    }
    
}
