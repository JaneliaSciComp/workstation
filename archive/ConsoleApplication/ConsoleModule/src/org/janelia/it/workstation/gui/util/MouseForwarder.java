package org.janelia.it.workstation.gui.util;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;

/**
 * Forwards events to some target, usually the component's parent.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MouseForwarder extends MouseHandler {

	private String name;
	private JComponent target;

	public MouseForwarder(JComponent target, String name) {
		this.name = name;
		this.target = target;
	}

//	@Override
//	public void mouseClicked(MouseEvent e) {
//		super.mouseClicked(e);
//		forward(e, "mouseClicked");
//	}

	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		forward(e, "mousePressed");
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		forward(e, "mouseReleased");
	}

//	@Override
//	public void mouseEntered(MouseEvent e) {
//		super.mouseEntered(e);
//		forward(e, "mouseEntered");
//	}
//
//	@Override
//	public void mouseExited(MouseEvent e) {
//		super.mouseExited(e);
//		forward(e, "mouseExited");
//	}

	private void forward(MouseEvent e, String eventName) {
		if (e.isConsumed()) return;
//		System.out.println("FORWARD "+eventName+" "+name);
    	target.dispatchEvent(e);
	}	
}
