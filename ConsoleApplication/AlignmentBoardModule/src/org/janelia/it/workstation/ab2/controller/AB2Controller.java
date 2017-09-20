package org.janelia.it.workstation.ab2.controller;

/*

This controller is intended to receive a stream of all events in the AB2 system. To make the controller
thread-safe, events are added to a thread-safe queue, and handled in a separate thread.

The EventHandler class handles certain non-controller specific Events, and then forwards all other Events to the
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

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.ab2.event.AB2ChangeModeEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.controller.AB2CompositionMode;
import org.janelia.it.workstation.ab2.controller.AB2ControllerMode;
import org.janelia.it.workstation.ab2.controller.AB2View3DMode;
import org.janelia.it.workstation.ab2.model.AB2DomainObject;
import org.janelia.it.workstation.ab2.renderer.AB2SimpleCubeRenderer;
import org.janelia.it.workstation.ab2.renderer.AB2SkeletonRenderer;

public class AB2Controller implements GLEventListener {
    private ConcurrentLinkedQueue<AB2Event> eventQueue;
    private ConcurrentLinkedQueue<AB2Event> waitQueue;
    private ScheduledExecutorService controllerExecutor;
    private ScheduledFuture<?> controllerHandle;
    private EventHandler eventHandler;
    private Map<Class, AB2ControllerMode> modeMap=new HashMap<>();
    private AB2ControllerMode currentMode;
    private GLJPanel gljPanel;
    private AB2DomainObject domainObject;

    public AB2Controller() {
        eventQueue=new ConcurrentLinkedQueue<>();
        waitQueue=new ConcurrentLinkedQueue<>();
        controllerExecutor=Executors.newSingleThreadScheduledExecutor();
        eventHandler=new EventHandler();
        populateModeMap();
    }

    public void setDomainObject(AB2DomainObject domainObject) {
        this.domainObject=domainObject;
    }

    public AB2DomainObject getDomainObject() {
         return domainObject;
    }

    public void setGljPanel(GLJPanel gljPanel) {
        this.gljPanel=gljPanel;
    }

    public void repaint() {
        gljPanel.repaint();
    }

    private void populateModeMap() {
        modeMap.put(AB2View3DMode.class, new AB2View3DMode(this, new AB2SimpleCubeRenderer()));
        modeMap.put(AB2CompositionMode.class, new AB2CompositionMode(this));
        modeMap.put(AB2SkeletonMode.class, new AB2SkeletonMode(this, new AB2SkeletonRenderer()));
    }

    public void start() {
        if (controllerHandle!=null) {
            return;
        } else {
            currentMode=modeMap.get(AB2SkeletonMode.class);
            currentMode.start();
            controllerHandle=controllerExecutor.scheduleWithFixedDelay(eventHandler, 500, 500, TimeUnit.MICROSECONDS);
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

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        if (currentMode!=null) {
            currentMode.init(glAutoDrawable);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        if (currentMode!=null) {
            currentMode.dispose(glAutoDrawable);
        }
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        if (currentMode!=null) {
            currentMode.display(glAutoDrawable);
        }
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {
        if (currentMode!=null) {
            currentMode.reshape(glAutoDrawable, i, i1, i2, i3);
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
