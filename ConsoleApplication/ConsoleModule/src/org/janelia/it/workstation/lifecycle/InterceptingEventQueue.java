/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.lifecycle;

import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgrActivityLogging;
import org.janelia.it.workstation.shared.util.SystemInfo;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgrActivityLogging;
import org.janelia.it.workstation.shared.util.SystemInfo;

/**
 * This is enabled at startup.  It will wrap the traditional event Queue,
 * and capture logs of target events coming from it.
 *
 * @author fosterl
 */
public class InterceptingEventQueue extends EventQueue implements MessageSource {
    private List<String> messages = new ArrayList<>();

    public InterceptingEventQueue() {
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)event;            
            if (me.paramString().startsWith(ReportRunner.MOUSE_EVENT_DISCRIMINATOR)) {
                // Assumption: these relative X,Y coords are relative to the overall JFrame, since no other semantic data is yet available.
                String message = me.getX() + "," + me.getY() + ":" + me.getXOnScreen() + "," + me.getYOnScreen() + ":" + new Date().getTime();
                synchronized (this) {
                    getMessages().add(message);
                }
            }
        }
        try {
            super.dispatchEvent(event);
        } catch (Throwable t) {
            String message = "InterceptingEventQueue Error:" + SystemInfo.getFreeSystemMemory() + "/" + SystemInfo.getTotalSystemMemory();
            System.err.println(message);
            System.err.println("Event is: " + event.getClass().toString());
            SessionMgrActivityLogging activityLogging = new SessionMgrActivityLogging();
            activityLogging.logToolEvent(new ToolString("EventQueue"), new CategoryString("excep:mem"), new ActionString(message));
            t.printStackTrace();
            throw t;
        }
    }
    
    @Override
    public synchronized void setMessages(List<String> messages) {
        this.messages = messages;
    }
    
    @Override
    public synchronized List<String> getMessages() {
        return messages;
    }
}

