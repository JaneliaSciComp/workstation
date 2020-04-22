package org.janelia.workstation.controller;

import com.google.common.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

public class EventBusRegistry {
    private Map<EventBusType, EventBus> eventRegistry;
    private static final EventBusRegistry instance = new EventBusRegistry();

    public static EventBusRegistry getInstance() {
        return instance;
    }

    public EventBusRegistry() {
        eventRegistry = new HashMap<>();
        EventBus sampleWorkspaceBus = new EventBus();
        eventRegistry.put(EventBusType.SAMPLEWORKSPACE, sampleWorkspaceBus);

        EventBus annotationBus = new EventBus();
        eventRegistry.put(EventBusType.ANNOTATION, annotationBus);

        EventBus viewStateBus = new EventBus();
        eventRegistry.put(EventBusType.VIEWSTATE, viewStateBus);

        EventBus selectionStateBus = new EventBus();
        eventRegistry.put(EventBusType.SELECTION, selectionStateBus);
    }

    public EventBus getEventRegistry(EventBusType type) {
        return eventRegistry.get(type);
    }

    public enum EventBusType {
        SAMPLEWORKSPACE, ANNOTATION, VIEWSTATE, SELECTION, IMAGERY, SCENEMANAGEMENT
    }
}
