/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * This collects events for forwarding in batch.
 *
 * @author fosterl
 */
public class ReportRunner implements Runnable {
    public static final String MOUSE_EVENT_DISCRIMINATOR = "MOUSE_CLICKED";
    public static final String BUTTON_EVENT_DISCRIMINATOR = "BUTTON_CLICKED";
    private static final int SLEEP_INTERVAL_S = 15 * 1000;
    private static final ToolString TOOL_STRING = new ToolString("MBTN");
    private static final CategoryString CATEGORY_STRING = new CategoryString("CLIK");

    private List<MessageSource> queue;
    private List<String> discriminators;

    public ReportRunner(List<MessageSource> queue, List<String> discriminators) {
        this.queue = queue;
        this.discriminators = discriminators;

        Thread t = new Thread(this);
        t.setName("InterceptingEventQueueThread");
        t.start();
    }

    public void run() {
        SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        while (true) {

            // Get rid of the sensitive stuff.
            List<List<String>> latest = new ArrayList<List<String>>();
            // Avoid entanglement with event queue, via sync block.
            for (MessageSource ms : queue) {
                latest.add(ms.getMessages());
                ms.setMessages(new ArrayList<String>());
            }

            try {
                Thread.sleep(SLEEP_INTERVAL_S);

                // Make a fairly brief action string.
                for (int i = 0; i < latest.size(); i++) {
                    List<String> messages = latest.get(i);
                    if (messages.isEmpty()) {
                        continue;
                    }
                    String discriminator = discriminators.get(i);
                    sessionMgr.logBatchToolEvent(TOOL_STRING, CATEGORY_STRING, discriminator, messages);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
