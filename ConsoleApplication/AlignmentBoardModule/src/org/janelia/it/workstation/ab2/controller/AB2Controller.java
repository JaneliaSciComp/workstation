package org.janelia.it.workstation.ab2.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.ab2.event.AB2ChangeModeEvent;
import org.janelia.it.workstation.ab2.event.AB2DomainObjectUpdateEvent;
import org.janelia.it.workstation.ab2.event.AB2Event;
import org.janelia.it.workstation.ab2.event.AB2EventHandler;
import org.janelia.it.workstation.ab2.event.AB2SampleAddedEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.janelia.it.workstation.ab2.model.AB2DomainObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2Controller implements GLEventListener, AB2EventHandler {

    Logger logger= LoggerFactory.getLogger(AB2Controller.class);

    private static AB2Controller instance;
    private ConcurrentLinkedQueue<AB2Event> eventQueue;
    private ConcurrentLinkedQueue<AB2Event> waitQueue;
    private ScheduledExecutorService controllerExecutor;
    private ScheduledFuture<?> controllerHandle;
    private EventHandler eventHandler;
    private Map<Class<?>, AB2ControllerMode> modeMap=new HashMap<>();
    private AB2ControllerMode currentMode;
    private GLJPanel gljPanel;
    private AB2DomainObject domainObject;
    private int pickCounter=0;
    private AB2UserContext userContext=new AB2UserContext();
    private boolean needsRepaint=false;

    public static AB2Controller getController() {
        if (instance==null) {
            instance=new AB2Controller();
        }
        return instance;
    }

    private AB2Controller() {
        eventQueue=new ConcurrentLinkedQueue<>();
        waitQueue=new ConcurrentLinkedQueue<>();
        controllerExecutor=Executors.newSingleThreadScheduledExecutor();
        eventHandler=new EventHandler();
        populateModeMap();
    }

    public AB2UserContext getUserContext() {
        return userContext;
    }

    public synchronized int getNextPickIndex() {
        pickCounter++;
        return pickCounter;
    }

    public boolean needsRepaint() {
        return needsRepaint;
    }

    public void setNeedsRepaint(boolean needsRepaint) {
        this.needsRepaint = needsRepaint;
    }

    // This design is being changed so that actors handle their own selection events
//    public void setPickEvent(int index, AB2Event pickEvent) {
//        logger.info("Setting pickIndex="+index+" to AB2Event type="+pickEvent.getClass().getName());
//        pickEventLookup.put(index,pickEvent);
//    }
//
//    public AB2Event getPickEvent(int index) {
//        AB2Event pickEvent=pickEventLookup.get(index);
//        logger.info("Returning pickEvent for pickIndex="+index+" type="+pickEvent.getClass().getName());
//        return pickEvent;
//    }

    public void setDomainObject(AB2DomainObject domainObject) {
        this.domainObject=domainObject;
        processEvent(new AB2DomainObjectUpdateEvent());
    }

    public AB2DomainObject getDomainObject() {
         return domainObject;
    }

    public void setGljPanel(GLJPanel gljPanel) {
        this.gljPanel=gljPanel;
    }

    public GLJPanel getGljPanel() {
        return gljPanel;
    }

    private void repaint() {
        gljPanel.repaint();
        needsRepaint=false;
    }

    private void populateModeMap() {
        //modeMap.put(AB2View3DMode.class, new AB2View3DMode(this, new AB2SimpleCubeRenderer()));
        //modeMap.put(AB2CompositionMode.class, new AB2CompositionMode(this));
        //modeMap.put(AB2SkeletonMode.class, new AB2SkeletonMode(this, new AB2SkeletonRenderer()));
        modeMap.put(AB2SampleBasicMode.class, new AB2SampleBasicMode(this));
    }

    public void start() {
        if (controllerHandle!=null) {
            return;
        } else {
            currentMode=modeMap.get(AB2SampleBasicMode.class);
            //currentMode=modeMap.get(AB2SkeletonMode.class);
            currentMode.start();
            controllerHandle=controllerExecutor.scheduleWithFixedDelay(eventHandler, 1000, 10, TimeUnit.MILLISECONDS);
        }
    }

    public AB2ControllerMode getCurrentMode() { return currentMode; }

    public void shutdown() {
        if (controllerHandle!=null) {
            controllerHandle.cancel(true);
        }
        controllerHandle=null;
        for (AB2ControllerMode mode : modeMap.values()) {
            mode.shutdown();
        }
    }

    public void processEvent(AB2Event event) {
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
        logger.info(" &&&&&&&&&&&&&& DISPOSE CALLED");
        if (currentMode!=null) {
            currentMode.dispose(glAutoDrawable);
        }
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
//        if (!startDelayComplete) {
//            try { Thread.sleep(500); } catch (Exception ex) {}
//            startDelayComplete=true;
//        }
        if (currentMode != null) {
            currentMode.display(glAutoDrawable);
        }
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        // The applyGlWindowResize() method is to support screen-size independent actors, such as text actors.
        // It is not related to the screen-size dependent resize hierarchy.
        GLAbstractActor.applyGlWindowResize(width, height);
        if (currentMode!=null) {
            currentMode.reshape(glAutoDrawable, x, y, width, height);
        }
    }

    public int getGlWidth() { return gljPanel.getSurfaceWidth(); }

    public int getGlHeight() { return gljPanel.getSurfaceHeight(); }

    private class EventHandler implements Runnable {

        public void run() {
            //logger.info("eventQueue size="+eventQueue.size());
            while (!eventQueue.isEmpty()) {
                //logger.info("EventHandler run() queue size="+eventQueue.size());
                try {
                    AB2Event event = eventQueue.poll();
                    if (event != null) {
                        if (event instanceof AB2ChangeModeEvent) {
                            Class<?> targetModeClass = ((AB2ChangeModeEvent) event).getNewMode();
                            AB2ControllerMode targetMode = modeMap.get(targetModeClass);
                            if (!targetMode.equals(currentMode)) {
                                currentMode.stop();
                                drainWaitQueueToEventQueue();
                                currentMode = targetMode;
                                currentMode.start();
                            }
                        }
                        else if (event instanceof AB2SampleAddedEvent) {
                            logger.info("EventHandler run() handling AB2SampleAddedEvent");
                            Class<?> targetModeClass = AB2SampleBasicMode.class;
                            if (currentMode.getClass().equals(targetModeClass)) {
                                logger.info("EventHandler run() passing AB2SampleAddedEvent to currentMode process()");
                                currentMode.processEvent(event);
                            }
                            else {
                                processEvent(new AB2ChangeModeEvent(AB2SampleBasicMode.class));
                                processEvent(event); // put this back in queue, to be processed after mode change
                            }
                        }
                        else {
                            currentMode.processEvent(event);
                        }
                    }
                }
                catch (Throwable t) {
                    FrameworkImplProvider.handleException(t);
                }
            }
            if (needsRepaint) {
                repaint();
            }
        }

    }

}
