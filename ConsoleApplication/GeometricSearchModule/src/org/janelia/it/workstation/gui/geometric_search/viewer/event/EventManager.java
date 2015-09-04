package org.janelia.it.workstation.gui.geometric_search.viewer.event;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerGLPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by murphys on 8/20/2015.
 */
public class EventManager {

    private static VoxelViewerGLPanel viewer;

    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

    private static Map<Object, List<VoxelViewerEventListener>> listenerMap= new HashMap<>();

    public static VoxelViewerGLPanel getViewer() {
        return viewer;
    }

    public static void setViewer(VoxelViewerGLPanel viewer) {
        EventManager.viewer = viewer;
    }

    public static synchronized void addListener(Object object, VoxelViewerEventListener listener) {
        List<VoxelViewerEventListener> listeners=listenerMap.get(object);
        if (listeners==null) {
            listeners=new ArrayList<>();
            listenerMap.put(object, listeners);
        }
        listeners.add(listener);
    }

    public static void sendEvent(Object object, VoxelViewerEvent event) {
        logger.info("*** sending event from "+object.getClass().getName()+" of type="+event.getClass().getName());
        if (event instanceof ActorModifiedEvent) {
            viewer.refresh();
        } else {
            List<VoxelViewerEventListener> listeners = listenerMap.get(object);
            if (listeners != null) {
                for (VoxelViewerEventListener listener : listeners) {
                    logger.info("*** >>> sending event to listener=" + listener.getClass().getName());
                    listener.processEvent(event);
                }
            } else {
                logger.error("Could not find listenerMap for object type=" + object.getClass().getName());
            }
            viewer.refreshIfPending();
        }
    }

}
