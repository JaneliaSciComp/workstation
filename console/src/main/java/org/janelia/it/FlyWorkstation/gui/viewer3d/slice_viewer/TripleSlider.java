package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Slider widget with three handles, for adjusting 
 * black level, gamma, and white level.
 * 
 * @author Christopher M. Bruns
 *
 */
public class TripleSlider extends CombinedJSliderHandle
implements MouseListener, MouseMotionListener
{
	private static final long serialVersionUID = 1L;

	private CombinedJSliderHandle blackSlider = new CombinedJSliderHandle();
	private CombinedJSliderHandle whiteSlider = new CombinedJSliderHandle();
	private CombinedJSliderHandle gammaSlider = new CombinedJSliderHandle();
	private CombinedJSliderHandle activeSlider = null;
	// Group child sliders for bulk updating
	private CombinedJSliderHandle allSliders[] = {blackSlider, gammaSlider, whiteSlider};
	// Padding in pixels at left and right sides of this JSlider component
	// beyond which a handle center may not go
	private int leftPad = 25;
	private int rightPad = 25;
	private int handlePixels = 28;
	
	public TripleSlider() {
		// Only the parent slider paints its track
		blackSlider.setPaintTrack(true);
		gammaSlider.setPaintTrack(true);
		whiteSlider.setPaintTrack(true);
		addMouseMotionListener(this);
		addMouseListener(this);
		
		// Keep the black and white handles from passing one another
		blackSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// System.out.println("black "+blackSlider.getValue());
				int min = blackSlider.getValue() + 1;
				whiteSlider.setMinimum(min);
				updateSliderBounds();
			}});
		whiteSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// System.out.println("white "+whiteSlider.getValue());
				int max = whiteSlider.getValue() - 1;
				blackSlider.setMaximum(max);
				updateSliderBounds();
			}});
		setMaximum(255);
		setMinimum(0);
		setBlackValue(0);
		setWhiteValue(255);
	}
	
	public void addBlackChangeListener(ChangeListener listener) {
		blackSlider.addChangeListener(listener);
	}
	
	@Override
	public void addChangeListener(ChangeListener listener) {
		for (JSlider slider : allSliders) {
				slider.addChangeListener(listener);
		}
	}
	
	public void addWhiteChangeListener(ChangeListener listener) {
		whiteSlider.addChangeListener(listener);
	}
	
	public void addGammaChangeListener(ChangeListener listener) {
		gammaSlider.addChangeListener(listener);
	}

	protected void delegateMouseEvent(MouseEvent event) 
	{
		/*
		// Use recently activated slider
		CombinedJSliderHandle slider = activeSlider;
		// ...or whatever slider is near the mouse
		if (slider == null)
			slider = getClosestHandle(event);
		if (slider == null)
			return;
		int dx = slider.getBounds().x;
		MouseEvent e = new MouseEvent(
				(Component)event.getSource(),
				event.getID(),
				event.getWhen(),
				event.getModifiers(),
				event.getX() - dx,
				event.getY(),
				event.getXOnScreen(),
				event.getYOnScreen(),
				event.getClickCount(),
				event.isPopupTrigger(),
				event.getButton());
		slider.processEvent(e);
		*/
	}
	
	public BoundedRangeModel getBlackModel() {
		return blackSlider.getModel();
	}
	
	public int getBlackValue() {
		return blackSlider.getValue();
	}
	
	// Figure out which sub-slider the user might be trying to interact with
	protected CombinedJSliderHandle getClosestHandle(MouseEvent event) 
	{
		double mousePos;
		int sliderSize;
		if (getOrientation() == JSlider.HORIZONTAL) {
			mousePos = event.getX() - leftPad;
			sliderSize = getWidth() - leftPad - rightPad;
		}
		else {
			mousePos = event.getY() - leftPad;
			sliderSize = getHeight() - leftPad - rightPad;
		}
		mousePos = mousePos / sliderSize;
		// TODO - assumes minimum is zero
		double blackPos = blackSlider.getValue() / (double)blackSlider.getMaximum();
		double whitePos = whiteSlider.getValue() / (double)whiteSlider.getMaximum();
		double gammaPos = gammaSlider.getValue() / (double)gammaSlider.getMaximum();
		// adjust to place gamma between black and white
		gammaPos = blackPos + gammaPos*(whitePos - blackPos);
		double dPos = Math.abs(blackPos - mousePos);
		CombinedJSliderHandle result = blackSlider;
		double dPos2 = Math.abs(gammaPos - mousePos);
		/* TODO gamma
		if (dPos2 < dPos) {
			result = gammaSlider;
			dPos = dPos2;
		}
		*/
		dPos2 = Math.abs(whitePos - mousePos);
		if (dPos2 < dPos) {
			result = whiteSlider;
			dPos = dPos2;
		}
		
		if (result == whiteSlider)
			System.out.println("white slider");
		else if (result == blackSlider)
			System.out.println("black slider");
		
		return result;
	}
	
	public BoundedRangeModel getGammaModel() {
		return gammaSlider.getModel();
	}
	
	public int getGammaValue() {
		return gammaSlider.getValue();
	}
	
	public BoundedRangeModel getWhiteModel() {
		return whiteSlider.getModel();
	}
	
	public int getWhiteValue() {
		return whiteSlider.getValue();
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		/*
		// black slider
		Rectangle r = blackSlider.getBounds();
		g.translate(r.x, r.y);
		blackSlider.paint(g);		
		// white slider
		r = whiteSlider.getBounds();
		g.translate(r.x, r.y); // only on Mac?
		whiteSlider.paint(g);
		// TODO - gamma slider
		 * 
		 */
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		delegateMouseEvent(event);
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent event) {}

	@Override
	public void mouseClicked(MouseEvent event) {
		delegateMouseEvent(event);
	}

	@Override
	public void mouseEntered(MouseEvent event) {}

	@Override
	public void mouseExited(MouseEvent event) {}

	@Override
	public void mousePressed(MouseEvent event) {
		activeSlider = getClosestHandle(event);
		delegateMouseEvent(event);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		delegateMouseEvent(event);
		activeSlider = null;
	}
	
	public void setBlackValue(int value) {
		blackSlider.setValue(value);
	}
	
	public void setWhiteValue(int value) {
		whiteSlider.setValue(value);
	}
	
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		System.out.println("set bounds 1");
		assert getOrientation() == JSlider.HORIZONTAL;
		updateSliderBounds();
	}
	
	@Override
	public void setBounds(Rectangle r) {
		setBounds(r.x, r.y, r.width, r.height);
	}
	
	@Override
	public void setExtent(int extent) {
		for (JSlider slider : allSliders) {
				slider.setExtent(extent);
		}
	}
	
	@Override
	public void setInverted(boolean inverted) {
		for (JSlider slider : allSliders)
				slider.setInverted(inverted);
	}
	
	@Override
	public void setMaximum(int maximum) {
		whiteSlider.setMaximum(maximum);
		if (maximum != getMaximum())
			whiteSlider.setValue(maximum);
		super.setMaximum(maximum);
	}

	@Override
	public void setMinimum(int minimum) {
		blackSlider.setMinimum(minimum);
		if (minimum != getMinimum())
			blackSlider.setValue(minimum);
		super.setMinimum(minimum);
	}

	@Override
	public void setOrientation(int orientation) {
		for (JSlider slider : allSliders)
				slider.setOrientation(orientation);
	}

	@Override
	public void setSize(int width, int height) {
		System.out.println("set size 1");
		// TODO this is wrong
		for (JSlider slider : allSliders)
				slider.setSize(width, height);
	}
	
	@Override
	public void setSize(Dimension d) {
		setSize(d.width, d.height);
	}
	
	@Override
	public void updateUI() {
		super.updateUI();
		if (allSliders == null) {
			super.updateUI();
			return;
		}
		for (JSlider slider : allSliders)
				slider.updateUI();
	}

	protected void updateSliderBounds() {
		Rectangle r = getBounds();
		int fieldWidth = r.width - 2*handlePixels - leftPad - rightPad;
		// Black/left slider
		double blkShrink = (whiteSlider.getValue() - getMinimum()) / (double)(getMaximum() - getMinimum());
		int blkW = leftPad + rightPad + (int)Math.round(fieldWidth * blkShrink);
		blackSlider.setBounds(r.x, r.y, blkW, r.height);
		// System.out.println("black bounds "+r.x+", "+blkW);
		// White/right slider
		double whtShrink = (getMaximum() - blackSlider.getValue()) / (double)(getMaximum() - getMinimum());
		int whtW = leftPad + rightPad + (int)Math.round(fieldWidth * whtShrink);
		int whtX = 2*handlePixels + r.x + fieldWidth - whtW + leftPad + rightPad;
		whiteSlider.setBounds(whtX, r.y, whtW, r.height);
		// System.out.println("white bounds "+whtX+", "+whtW);
		System.out.println("values: " + getMinimum()
				+ ", "+ blackSlider.getValue()
				+ ", " + whiteSlider.getValue()
				+ ", " + getMaximum());
		System.out.println("pixels: " + r.x
				+ ", "+ (r.x + blkW)
				+ ", " + whtX
				+ ", " + (whtX + whtW)
				+ ", " + (r.x + r.width));
	}

}
