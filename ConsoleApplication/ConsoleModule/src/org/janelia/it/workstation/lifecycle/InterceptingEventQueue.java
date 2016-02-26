/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.lifecycle;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * This is enabled at startup.  It will wrap the traditional event Queue,
 * and capture logs of target events coming from it.
 *
 * @author fosterl
 */
public class InterceptingEventQueue extends EventQueue {
    private static final int SLEEP_INTERVAL_S = 15 * 1000;
    private static final ToolString TOOL_STRING = new ToolString("MBTN");
    private static final String MOUSE_EVENT_DISCRIMINATOR = "MOUSE_CLICKED";
    private static final CategoryString CATEGORY_STRING = new CategoryString("CLIK");

    private List<Long> eventTimes = new ArrayList<>();

    public InterceptingEventQueue() {
        Thread t = new Thread(new ReportRunner(this));
        t.setName("InterceptingEventQueueThread");
        t.start();
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)event;
            if (me.paramString().startsWith(MOUSE_EVENT_DISCRIMINATOR)) {
                synchronized (this) {
                    getEventTimes().add(new Date().getTime());
                }
            }
        }
        super.dispatchEvent(event);
    }
    
    private void setEventTimes(List<Long> eventTimes) {
        this.eventTimes = eventTimes;
    }
    
    private List<Long> getEventTimes() {
        return eventTimes;
    }

    public static class ReportRunner implements Runnable {
        private InterceptingEventQueue queue;
        public ReportRunner(InterceptingEventQueue queue) {
            this.queue = queue;
        }

        public void run() {
            SessionMgr sessionMgr = SessionMgr.getSessionMgr();
            while (true) {

                // Get rid of the sensitive stuff.
                List<Long> latest = null;
                synchronized (this) {
                    // Avoid entanglement with event queue, with sync block.
                    latest = new ArrayList<>(queue.getEventTimes());
                    queue.setEventTimes(new ArrayList<Long>());
                }

                try {
                    Thread.sleep(SLEEP_INTERVAL_S);

                    for (Long eventTime: latest) {
                        // Log as little as possible about the event.
                        //String paramStr = ae.paramString();
                        ActionString as = new ActionString(MOUSE_EVENT_DISCRIMINATOR + ":" + eventTime);
                        sessionMgr.logToolEvent(TOOL_STRING, CATEGORY_STRING, as);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

