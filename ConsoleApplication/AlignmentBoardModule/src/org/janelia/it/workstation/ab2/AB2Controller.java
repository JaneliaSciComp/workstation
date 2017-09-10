package org.janelia.it.workstation.ab2;

/*

This controller is intended to receive a stream of all events in the AB2 system. To make the controller
thread-safe, events are added to a thread-safe queue, and handled in a separate thread.

*/

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

public class AB2Controller implements Runnable {
    private ConcurrentLinkedQueue<AB2Event> eventQueue;
    private ScheduledExecutorService controllerExecutor;
    private ScheduledFuture<?> controllerHandle;

    public AB2Controller() {
        eventQueue=new ConcurrentLinkedQueue<>();
        controllerExecutor=Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        if (controllerHandle!=null) {
            return;
        } else {
            controllerHandle=controllerExecutor.scheduleWithFixedDelay(this, 1, 1, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        if (controllerHandle!=null) {
            controllerHandle.cancel(true);
        }
        controllerHandle=null;
    }

    public void addEvent(AB2Event event) {
        eventQueue.add(event);
    }

    public void run() {
        while(!eventQueue.isEmpty()) {
            AB2Event event = eventQueue.poll();
            if (event!=null) {

            }
        }
    }

}
