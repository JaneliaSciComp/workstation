package org.janelia.it.FlyWorkstation.gui.util;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Extend this class instead of MouseAdapter to simplify mouse click management.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MouseHandler implements MouseListener {

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // Right click
        	popupTriggered(e);
        }
	}

	@Override
	public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // Right click
        	popupTriggered(e);
        }
        else if (e.getClickCount()>=2 && e.getClickCount() % 2 == 0 && e.getButton() == MouseEvent.BUTTON1 && 
        		(e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
            // Double click
        	// We check for mod 2 because if the user is clicking fast, the click count may not get reset between double-clicks
        	doubleLeftClicked(e);
        }
        else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
            // Single click
        	singleLeftClicked(e);
        }
	}

	/**
	 * Override this method to get notified when the mouse enters the component.
	 * @param e
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * Override this method to show a popup menu when the mouse leaves the component.
	 * @param e
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	/**
	 * Override this method to show a popup menu when the user clicks their popup trigger.
	 * @param e
	 */
	protected void popupTriggered(MouseEvent e) {
	}
    
    /**
     * Override this method to do something when the user left clicks.
     *
     * @param e
     */
    protected void singleLeftClicked(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user double clicks.
     *
     * @param e
     */
    protected void doubleLeftClicked(MouseEvent e) {
    }


}
