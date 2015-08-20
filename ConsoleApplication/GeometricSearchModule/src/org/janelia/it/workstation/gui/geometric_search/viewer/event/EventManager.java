package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by murphys on 8/20/2015.
 */
public class EventManager {

    private static Map<Object, List<VoxelViewerEventListener>> listenerMap= new HashMap<>();

    public static synchronized void addListener(Object object, VoxelViewerEventListener listener) {
        List<VoxelViewerEventListener> listeners=listenerMap.get(object);
        if (listeners==null) {
            listeners=new ArrayList<>();
            listenerMap.put(object, listeners);
        }
        listeners.add(listener);
    }

    public static void sendEvent(Object object, VoxelViewerEvent event) {
        List<VoxelViewerEventListener> listeners=listenerMap.get(object);
        if (listeners!=null) {
            for (VoxelViewerEventListener listener : listeners) {
                listener.processEvent(event);
            }
        }
    }

}
