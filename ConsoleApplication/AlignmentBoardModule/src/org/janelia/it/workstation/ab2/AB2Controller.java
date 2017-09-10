package org.janelia.it.workstation.ab2;

/*

This controller is intended to receive a stream of all events in the AB2 system. To make the controller
thread-safe, events are added to a thread-safe queue, and handled in a separate thread.

The EventHandler class handles certain non-mode specific Events, and then forwards all other Events to the
current Mode controller.

Events which are not handled by the current Mode controller (and implicitly, also not handled by the EventHandler)
are placed in the waitQueue, to be handled by the next Mode controller.

*/

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.janelia.it.workstation.ab2.event.AB2ChangeModeEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.mode.AB2CompositionMode;
import org.janelia.it.workstation.ab2.mode.AB2ControllerMode;
import org.janelia.it.workstation.ab2.mode.AB2View3DMode;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

public class AB2Controller {
    private AB2Renderer renderer;
    private ConcurrentLinkedQueue<AB2Event> eventQueue;
    private ConcurrentLinkedQueue<AB2Event> waitQueue;
    private ScheduledExecutorService controllerExecutor;
    private ScheduledFuture<?> controllerHandle;
    private EventHandler eventHandler;
    private Map<Class, AB2ControllerMode> modeMap=new HashMap<>();
    private AB2ControllerMode currentMode;

    public AB2Controller(AB2Renderer renderer) {
        this.renderer=renderer;
        eventQueue=new ConcurrentLinkedQueue<>();
        waitQueue=new ConcurrentLinkedQueue<>();
        controllerExecutor=Executors.newSingleThreadScheduledExecutor();
        eventHandler=new EventHandler();
        populateModeMap();
    }

    private void populateModeMap() {
        modeMap.put(AB2View3DMode.class, new AB2View3DMode(renderer));
        modeMap.put(AB2CompositionMode.class, new AB2CompositionMode(renderer));
    }

    public void start() {
        if (controllerHandle!=null) {
            return;
        } else {
            currentMode=modeMap.get(AB2View3DMode.class);
            currentMode.start();
            controllerHandle=controllerExecutor.scheduleWithFixedDelay(eventHandler, 1, 1, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        if (controllerHandle!=null) {
            controllerHandle.cancel(true);
        }
        controllerHandle=null;
        for (AB2ControllerMode mode : modeMap.values()) {
            mode.shutdown();
        }
    }

    public void addEvent(AB2Event event) {
        eventQueue.add(event);
    }

    public void drainWaitQueueToEventQueue() {
        while (!waitQueue.isEmpty()) {
            AB2Event event=waitQueue.poll();
            if (event!=null) {
                eventQueue.add(event);
            }
        }
    }

    private class EventHandler implements Runnable {

        public void run() {
            while (!eventQueue.isEmpty()) {
                AB2Event event = eventQueue.poll();
                if (event != null) {
                    if (event instanceof AB2ChangeModeEvent) {
                        Class targetModeClass=((AB2ChangeModeEvent) event).getNewMode();
                        AB2ControllerMode targetMode=modeMap.get(targetModeClass);
                        if (!targetMode.equals(currentMode)) {
                            currentMode.stop();
                            drainWaitQueueToEventQueue();
                            currentMode=targetMode;
                            currentMode.start();
                        }
                    } else {
                        currentMode.processEvent(event);
                    }
                }
            }
        }

    }


}
