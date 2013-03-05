package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.AWTEvent;
import java.awt.Graphics;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/** 
 * So I can call paintComponent from TripleSlider class
 * 
 * @author Christopher M. Bruns
 *
 */
class CombinedJSliderHandle extends JSlider
{
	private static final long serialVersionUID = 1L;

	public CombinedJSliderHandle() {
		BasicSliderUI sliderUi;
		sliderUi = (BasicSliderUI)getUI();
		// TODO - use UI to help compute 
		System.out.println(sliderUi.valueForXPosition(50));
		System.out.println(sliderUi.getClass().getCanonicalName());
	}
	
	// Expose paintComponent so other sliders can drive painting
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
	
	@Override
	public void processEvent(AWTEvent event) {
		super.processEvent(event);
	}
	
	@Override
	public void setValue(int value) {
		int newValue = value;
		if (newValue > 200) {
			newValue = 200;
			System.out.println("value = " + value + ", " + newValue);
		}
		super.setValue(newValue);
	}
	
	@Override
	public void setValueIsAdjusting(boolean isAdjusting) {
		super.setValueIsAdjusting(false);
	}
		
}
