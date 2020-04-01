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
        setEventRegistry(new HashMap<>());
        EventBus sampleWorkspaceBus = new EventBus();
        getEventRegistry().put(EventBusType.SAMPLEWORKSPACE, sampleWorkspaceBus);

        EventBus annotationBus = new EventBus();
        getEventRegistry().put(EventBusType.ANNOTATION, annotationBus);

        EventBus viewStateBus = new EventBus();
        getEventRegistry().put(EventBusType.VIEWSTATE, viewStateBus);
    }

    public Map<EventBusType, EventBus> getEventRegistry() {
        return eventRegistry;
    }

    public void setEventRegistry(Map<EventBusType, EventBus> eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public enum EventBusType {
        SAMPLEWORKSPACE, ANNOTATION, VIEWSTATE, SELECTION, IMAGERY, SCENEMANAGEMENT
    }
}
