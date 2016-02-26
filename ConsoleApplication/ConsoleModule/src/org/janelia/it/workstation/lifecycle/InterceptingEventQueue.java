/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.lifecycle;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * This is enabled at startup.  It will wrap the traditional event Queue,
 * and capture logs of target events coming from it.
 *
 * @deprecated this hook has no access to any semantic data. Low-level events are uninteresting.
 *
 * @author fosterl
 */
public class InterceptingEventQueue extends EventQueue {
    private static final int SLEEP_INTERVAL_S = 15 * 1000;
    private static final ToolString TOOL_STRING = new ToolString("TJWS");
    private static final CategoryString CATEGORY_STRING = new CategoryString("TBTN");

    private List<AWTEvent> events = new ArrayList<>();

    public InterceptingEventQueue() {
        Thread t = new Thread(new ReportRunner(events));
        t.setName("InterceptingEventQueueThread");
        t.start();
    }

    protected void dispatchEvent(AWTEvent event) {
        if (event instanceof ActionEvent) {
            synchronized (this) {
                events.add(event);
                Object src = event.getSource();
                System.out.println("Source of mouse event " + src.getClass().getName());
            }
        }
//        else {
//            System.out.println(event.getClass().getName());
//        }
        super.dispatchEvent(event);
    }

    public static class ReportRunner implements Runnable {
        private List<AWTEvent> events;
        public ReportRunner(List<AWTEvent> events) {
            this.events = events;
        }

        public void run() {
            SessionMgr sessionMgr = SessionMgr.getSessionMgr();
            while (true) {

                // Get rid of the sensitive stuff.
                List<AWTEvent> latest = null;
                synchronized (this) {
                    // Avoid entanglement with event queue.
                    latest = new ArrayList<>(events);
                    events.clear();
                }

                try {
                    Thread.sleep(SLEEP_INTERVAL_S);

                    for (AWTEvent ae: latest) {
                        // Log what we know about the event.
                        String actionCommand = ae.getSource().getClass().getSimpleName();
                        String paramStr = ae.paramString();
                        ActionString as = new ActionString(actionCommand+" "+paramStr);
                        sessionMgr.logToolEvent(TOOL_STRING, CATEGORY_STRING, as);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

