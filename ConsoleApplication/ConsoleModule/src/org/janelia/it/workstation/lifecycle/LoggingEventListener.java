/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.lifecycle;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractButton;

/**
 * Capture events from mouse clicks, generically.
 *
 * @author fosterl
 */
public class LoggingEventListener implements AWTEventListener, MessageSource {
    
    private List<String> messages = new ArrayList<>();

    @Override
    public void eventDispatched(AWTEvent event) {
        MouseEvent me = (MouseEvent) event;
        if (MouseEvent.MOUSE_RELEASED == me.getID()) {
            if (me.getSource() instanceof AbstractButton) {
                AbstractButton src = (AbstractButton) event.getSource();
                String message = me.getX() + "," + me.getY() + ":" + me.getXOnScreen() + "," + me.getYOnScreen() + ":" + new Date().getTime();
                synchronized(this) {
                    messages.add(message);
                }
                //System.out.println("Click Count: " + me.getClickCount() + ", JButton: " + src.getActionCommand() + ", Name: " + src.getName() + ", Text: " + src.getText() + ", tooltip: " + src.getToolTipText());
            }
//            else if (me.getSource() instanceof Button) {
//                Button src = (Button) event.getSource();
//                System.out.println("Click Count: " + me.getClickCount() + ", Button: " + src.getActionCommand() + ", Name: " + src.getName() + ", Label: " + src.getLabel());
//            }
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
