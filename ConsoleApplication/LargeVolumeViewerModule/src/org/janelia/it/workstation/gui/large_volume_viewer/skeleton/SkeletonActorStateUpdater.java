/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.workstation.gui.large_volume_viewer.RepaintListener;

/**
 * Relay for updating state listeners.  This removes the verbiage from the
 * class under study.
 * 
 * @author fosterl
 */
public class SkeletonActorStateUpdater {
    private List<RepaintListener> listeners = new ArrayList<>();
    
    public void addListener( RepaintListener listener ) {
        listeners.add(listener);
    }
    
    public void removeListener( RepaintListener listener ) {
        listeners.remove(listener);
    }
    
    public void update() {
        for (RepaintListener l: listeners) {
            l.repaint();
        }
    }
    
}
