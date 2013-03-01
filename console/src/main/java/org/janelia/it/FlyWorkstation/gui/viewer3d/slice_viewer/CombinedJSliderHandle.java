package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.AWTEvent;
import java.awt.Graphics;

import javax.swing.JSlider;

/** 
 * So I can call paintComponent from TripleSlider class
 * 
 * @author Christopher M. Bruns
 *
 */
class CombinedJSliderHandle extends JSlider
{
	private static final long serialVersionUID = 1L;

	// Expose paintComponent so other sliders can drive painting
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
	
	@Override
	public void processEvent(AWTEvent event) {
		super.processEvent(event);
	}
		
}
