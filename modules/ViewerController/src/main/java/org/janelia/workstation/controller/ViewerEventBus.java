package org.janelia.workstation.controller;

import com.google.common.eventbus.EventBus;

import javax.swing.*;

public class ViewerEventBus {
    private static final EventBus viewerbus = new EventBus();

    public static void postEvent(Object event) {
        SwingUtilities.invokeLater(()->viewerbus.post(event));
    }
    public static void unregisterForEvents(Object listener) {
        viewerbus.unregister(listener);
    }
    public static void registerForEvents(Object listener) {
        viewerbus.register(listener);
    }
}
