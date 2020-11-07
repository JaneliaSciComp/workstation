package org.janelia.workstation.controller.listener;

import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
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
        // logger.info("tolerant click!");
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
