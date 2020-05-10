package org.janelia.workstation.controller;

import com.google.common.eventbus.EventBus;

import java.util.HashMap;

public class ViewerEventBus {
    private static final EventBus viewerbus = new EventBus();

    public static void postEvent(Object event) {
        viewerbus.post(event);
    }
    public static void registerForEvents(Object listener) {
        viewerbus.register(listener);
    }
}
