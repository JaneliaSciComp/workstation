/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta;

import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputListener;

/**
 * Allow mouse click with a tiny amount of motion to count as a mouseClicked() event
 * Based on http://stackoverflow.com/questions/522244/making-a-component-less-sensitive-to-dragging-in-swing
 * @author brunsc
 */
public class TolerantMouseClickListener 
implements MouseInputListener
{
    private final int maxClickDistance;
    private final MouseInputListener target;
    
    private MouseEvent pressedEvent = null;
    
    public TolerantMouseClickListener(MouseInputListener target, int maxClickDistance) {
        this.maxClickDistance = maxClickDistance;
        this.target = target;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        //do nothing, handled by pressed/released handlers
    }

    private int getDragDistance(MouseEvent e) 
    {
        int distance = 0;
        distance += Math.abs(pressedEvent.getXOnScreen() - e.getXOnScreen());
        distance += Math.abs(pressedEvent.getYOnScreen() - e.getYOnScreen());
        return distance;
    }
    
    private boolean isClickRelease(MouseEvent releaseEvent) {
        if (pressedEvent == null)
            return false;
        int dist = getDragDistance(releaseEvent);
        if (dist > maxClickDistance)
            return false;
        // TODO: also time interval?
        return true;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pressedEvent = e;
        target.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) 
    {
        target.mouseReleased(e);
        
        if (pressedEvent == null)
            return;
        if (! isClickRelease(e))
            return;
        MouseEvent clickEvent = new MouseEvent(
                (Component) pressedEvent.getSource(),
                MouseEvent.MOUSE_CLICKED, 
                e.getWhen(), 
                pressedEvent.getModifiers(),
                pressedEvent.getX(), 
                pressedEvent.getY(), 
                pressedEvent.getXOnScreen(), 
                pressedEvent.getYOnScreen(),
                pressedEvent.getClickCount(), 
                pressedEvent.isPopupTrigger(), 
                pressedEvent.getButton());
        target.mouseClicked(clickEvent);
        pressedEvent = null;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        target.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        target.mouseExited(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (pressedEvent != null) {
            if (getDragDistance(e) <= maxClickDistance)
                return; //do not trigger drag yet (distance is in "click" perimeter)
            pressedEvent = null; // remember if we ever drag outside click radius
        }
        target.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        target.mouseMoved(e);
    }
    
}
