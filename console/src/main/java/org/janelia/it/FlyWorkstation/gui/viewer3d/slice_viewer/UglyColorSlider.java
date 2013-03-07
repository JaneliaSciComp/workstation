package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

public class UglyColorSlider extends JSlider 
{
	private static final long serialVersionUID = 1L;

	protected int channelIndex;
	protected ImageColorModel imageColorModel;

	// TODO replace value members with range model
	private int grayLevel = 127;
	private boolean updatingFromModel = false;

	private Slot updateSliderSlot = new Slot() {
		@Override
		public void execute() {
			updateSliderValuesFromModel();
		}
	};
	
	public UglyColorSlider(int channelIndex, ImageColorModel imageColorModel) 
	{
		this.channelIndex = channelIndex;
		this.imageColorModel = imageColorModel;
		imageColorModel.getColorModelChangedSignal().connect(updateSliderSlot);
		updateSliderValuesFromModel();
		initSlider();
	}
	
	public int getBlackLevel() {
		return getValue();
	}
	
	public int getGrayLevel() {
		return grayLevel;
	}
	
	public int getWhiteLevel() {
		return getValue() + getExtent();
	}
	
	private void initSlider() {
		setOrientation(HORIZONTAL);
	}
	
	public void setBlackLevel(int level) {
		setValue(level);
	}
	
	public void setGrayLevel(int level) {
		grayLevel = level;
		updateModelFromSliderValues();
	}
	
    @Override
    public void setValue(int value) {
        int oldValue = getValue();
        if (oldValue == value) {
            return;
        }

        // Compute new value and extent to maintain upper value.
        int oldExtent = getExtent();
        int newValue = Math.min(Math.max(getMinimum(), value), oldValue + oldExtent);
        int newExtent = oldExtent + oldValue - newValue;

        // Set new value and extent, and fire a single change event.
        getModel().setRangeProperties(newValue, newExtent, getMinimum(),
            getMaximum(), getValueIsAdjusting());
		updateModelFromSliderValues();
    }

	public void setWhiteLevel(int level) {
        // Compute new extent.
        int lowerValue = getValue();
        int newExtent = Math.min(Math.max(0, level - lowerValue), getMaximum() - lowerValue);
        
        // Set extent to set upper value.
        setExtent(newExtent);
		updateModelFromSliderValues();
	}
	
	private void updateSliderValuesFromModel() {
		if (imageColorModel == null)
			return;
		if (imageColorModel.getChannelCount() <= channelIndex)
			return;
		updatingFromModel = true;
		ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
		setMinimum(0);
		setMaximum(ccm.getDataMax()); // right edge of slider widget
		setBlackLevel(ccm.getBlackLevel());
		setWhiteLevel(ccm.getWhiteLevel());
		// convert gamma to gray level
		// first compute ratio between white and black
		float grayLevel = (float)Math.pow(0.5, 1.0/ccm.getGamma());
		grayLevel = getBlackLevel() + grayLevel*(getWhiteLevel() - getBlackLevel());
		setGrayLevel((int)Math.round(grayLevel));
		updatingFromModel = false;
	}
	
	private void updateModelFromSliderValues() {
		if (updatingFromModel) // don't change model while populating slider
			return;
		if (imageColorModel == null)
			return;
		if (imageColorModel.getChannelCount() <= channelIndex)
			return;
		ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
		ccm.setBlackLevel(getBlackLevel());
		ccm.setWhiteLevel(getWhiteLevel());
		// TODO gray
	}

    /**
	* Overrides the superclass method to install the UI delegate to draw three
	* thumbs.
	*/
	    @Override
	    public void updateUI() {
	        setUI(new UglyColorSliderUI(this));
	        // Update UI for slider labels. This must be called after updating the
	        // UI of the slider. Refer to JSlider.updateUI().
	        updateLabelUIs();
	    }

	
	
	
	static class UglyColorSliderUI extends BasicSliderUI
	{
		private enum Thumb {
			BLACK,
			GRAY,
			WHITE
		}
		
		// Colors for (eventual) track decoration
		private Color blackColor = Color.black;
		private Color whiteColor = Color.white;
		private Color grayColor = Color.gray;
		private Color rangeColor = Color.green; // TODO temporary
		
		// Black rectangle is the parent thumRect
		private Rectangle blackThumbRect;
		private Rectangle grayThumbRect;
		private Rectangle whiteThumbRect;
		
		private Thumb selectedThumb = Thumb.BLACK;
		
		private transient boolean blackDragging;
		private transient boolean grayDragging;
		private transient boolean whiteDragging;
		
		public UglyColorSliderUI(UglyColorSlider slider) {
			super(slider);
		}
		
	   @Override
	    protected void calculateThumbLocation() {
	        // Call superclass method for black thumb location.
	        super.calculateThumbLocation();
	        
	        UglyColorSlider colorSlider = (UglyColorSlider) slider;
	        
	        // Adjust upper value to snap to ticks if necessary.
	        if (slider.getSnapToTicks()) {
	        		int whiteLevel = colorSlider.getWhiteLevel();
	        		
	            int snappedWhiteValue = whiteLevel;
	            int majorTickSpacing = slider.getMajorTickSpacing();
	            int minorTickSpacing = slider.getMinorTickSpacing();
	            int tickSpacing = 0;
	            
	            if (minorTickSpacing > 0) {
	                tickSpacing = minorTickSpacing;
	            } else if (majorTickSpacing > 0) {
	                tickSpacing = majorTickSpacing;
	            }

	            if (tickSpacing != 0) {
	                // If it's not on a tick, change the value
	                if ((whiteLevel - slider.getMinimum()) % tickSpacing != 0) {
	                    float temp = (float)(whiteLevel - slider.getMinimum()) / (float)tickSpacing;
	                    int whichTick = Math.round(temp);
	                    snappedWhiteValue = slider.getMinimum() + (whichTick * tickSpacing);
	                }

	                if (snappedWhiteValue != whiteLevel) {
	                    colorSlider.setWhiteLevel(snappedWhiteValue - slider.getValue());
	                }
	            }
	        }
	        
	        // Calculate upper thumb location. The thumb is centered over its
	        // value on the track.
	        if (slider.getOrientation() == JSlider.HORIZONTAL) {
	            int whitePosition = xPositionForValue(colorSlider.getWhiteLevel());
	            whiteThumbRect.x = whitePosition - (whiteThumbRect.width / 2);
	            whiteThumbRect.y = trackRect.y;

	            int grayPosition = xPositionForValue(colorSlider.getGrayLevel());
	            grayThumbRect.x = grayPosition - (grayThumbRect.width / 2);
	            grayThumbRect.y = trackRect.y;
	        } else {
	            int whitePosition = yPositionForValue(colorSlider.getWhiteLevel());
	            whiteThumbRect.x = trackRect.x;
	            whiteThumbRect.y = whitePosition - (whiteThumbRect.height / 2);
	            
	            int grayPosition = yPositionForValue(colorSlider.getGrayLevel());
	            grayThumbRect.x = trackRect.x;
	            grayThumbRect.y = grayPosition - (grayThumbRect.height / 2);
	        }
	    }

		@Override
		protected void calculateThumbSize() {
			super.calculateThumbSize();
			grayThumbRect.setSize(thumbRect.width, thumbRect.height);
			whiteThumbRect.setSize(thumbRect.width, thumbRect.height);
		}
		
		@Override
		protected ChangeListener createChangeListener(JSlider slider) {
			return new ChangeHandler();
		}
		
	    /**
		* Returns a Shape representing a thumb.
		*/
	    private Shape createThumbShape(int width, int height) {
	        // Use circular shape.
	        Ellipse2D shape = new Ellipse2D.Double(0, 0, width, height);
	        return shape;
	    }
		  
		@Override
		protected TrackListener createTrackListener(JSlider slider) {
			return new ColorTrackListener();
		}
		
		@Override
		public void installUI(JComponent c) {
			grayThumbRect = new Rectangle();
			whiteThumbRect = new Rectangle();
			super.installUI(c);
			blackThumbRect = thumbRect;
		}
		
	    /**
		* Paints the slider. The selected thumb is always painted on top of the
		* other thumb.
		*/
	    @Override
	    public void paint(Graphics g, JComponent c) {
	        super.paint(g, c);
	        Rectangle clipRect = g.getClipBounds();
	        
	        if (selectedThumb == Thumb.WHITE) {
	            // Paint selected thumb last
	            if (clipRect.intersects(blackThumbRect)) {
	                paintBlackThumb(g);
	            }
	            if (clipRect.intersects(grayThumbRect)) {
	                paintGrayThumb(g);
	            }
	            if (clipRect.intersects(whiteThumbRect)) {
	                paintWhiteThumb(g);
	            }
	        } else if (selectedThumb == Thumb.GRAY) {
	            if (clipRect.intersects(blackThumbRect)) {
	                paintBlackThumb(g);
	            }
	            if (clipRect.intersects(whiteThumbRect)) {
	                paintWhiteThumb(g);
	            }
	            if (clipRect.intersects(grayThumbRect)) {
	                paintGrayThumb(g);
	            }	        		
	        } else {
	            if (clipRect.intersects(whiteThumbRect)) {
	                paintWhiteThumb(g);
	            }
	            if (clipRect.intersects(grayThumbRect)) {
	                paintGrayThumb(g);
	            }	        		
	            if (clipRect.intersects(blackThumbRect)) {
	                paintBlackThumb(g);
	            }
	        }
	    }
	    
	    /**
	    * Paints the thumb for the black value using the specified graphics object.
	    */
        private void paintBlackThumb(Graphics g) {
            Rectangle knobBounds = thumbRect;
            int w = knobBounds.width;
            int h = knobBounds.height;
            
            // Create graphics copy.
            Graphics2D g2d = (Graphics2D) g.create();

            // Create default thumb shape.
            Shape thumbShape = createThumbShape(w - 1, h - 1);

            // Draw thumb.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.translate(knobBounds.x, knobBounds.y);

            g2d.setColor(Color.black);
            g2d.fill(thumbShape);

            g2d.setColor(Color.lightGray);
            g2d.draw(thumbShape);
            
            // Dispose graphics.
            g2d.dispose();
        }

        private void paintGrayThumb(Graphics g) {
            Rectangle knobBounds = grayThumbRect;
            int w = knobBounds.width;
            int h = knobBounds.height;
            
            // Create graphics copy.
            Graphics2D g2d = (Graphics2D) g.create();

            // Create default thumb shape.
            Shape thumbShape = createThumbShape(w - 1, h - 1);

            // Draw thumb.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.translate(knobBounds.x, knobBounds.y);

            g2d.setColor(Color.gray);
            g2d.fill(thumbShape);

            g2d.setColor(Color.darkGray);
            g2d.draw(thumbShape);
            
            // Dispose graphics.
            g2d.dispose();
        }

        private void paintWhiteThumb(Graphics g) {
            Rectangle knobBounds = whiteThumbRect;
            int w = knobBounds.width;
            int h = knobBounds.height;
            
            // Create graphics copy.
            Graphics2D g2d = (Graphics2D) g.create();

            // Create default thumb shape.
            Shape thumbShape = createThumbShape(w - 1, h - 1);

            // Draw thumb.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.translate(knobBounds.x, knobBounds.y);

            g2d.setColor(Color.white);
            g2d.fill(thumbShape);

            g2d.setColor(Color.gray);
            g2d.draw(thumbShape);
            
            // Dispose graphics.
            g2d.dispose();
        }

	    @Override
	    public void paintThumb(Graphics g) {
	        // Do nothing.
	    }

	    @Override
	    public void paintTrack(Graphics g) {
	        // Draw track.
	        super.paintTrack(g);
	        
	        Rectangle trackBounds = trackRect;
	        
	        if (slider.getOrientation() == JSlider.HORIZONTAL) {
	            // Determine position of selected range by moving from the middle
	            // of one thumb to the other.
	            int blackX = thumbRect.x + (thumbRect.width / 2);
	            int upperX = whiteThumbRect.x + (whiteThumbRect.width / 2);
	            
	            // Determine track position.
	            int cy = (trackBounds.height / 2) - 2;

	            // Save color and shift position.
	            Color oldColor = g.getColor();
	            g.translate(trackBounds.x, trackBounds.y + cy);
	            
	            // Draw selected range.
	            g.setColor(rangeColor);
	            for (int y = 0; y <= 3; y++) {
	                g.drawLine(blackX - trackBounds.x, y, upperX - trackBounds.x, y);
	            }

	            // Restore position and color.
	            g.translate(-trackBounds.x, -(trackBounds.y + cy));
	            g.setColor(oldColor);
	            
	        } else {
	            // Determine position of selected range by moving from the middle
	            // of one thumb to the other.
	            int blackY = thumbRect.x + (thumbRect.width / 2);
	            int upperY = whiteThumbRect.x + (whiteThumbRect.width / 2);
	            
	            // Determine track position.
	            int cx = (trackBounds.width / 2) - 2;

	            // Save color and shift position.
	            Color oldColor = g.getColor();
	            g.translate(trackBounds.x + cx, trackBounds.y);

	            // Draw selected range.
	            g.setColor(rangeColor);
	            for (int x = 0; x <= 3; x++) {
	                g.drawLine(x, blackY - trackBounds.y, x, upperY - trackBounds.y);
	            }
	            
	            // Restore position and color.
	            g.translate(-(trackBounds.x + cx), -trackBounds.y);
	            g.setColor(oldColor);
	        }
	    }
	    
	    /**
	    * Moves the selected thumb in the specified direction by a block increment.
	    * This method is called when the user presses the Page Up or Down keys.
	    */
        public void scrollByBlock(int direction) {
            synchronized (slider) {
                int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
                if (blockIncrement <= 0 && slider.getMaximum() > slider.getMinimum()) {
                    blockIncrement = 1;
                }
                int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
                
                UglyColorSlider colorSlider = (UglyColorSlider) slider;
                if (selectedThumb == Thumb.WHITE) {
                    int oldValue = colorSlider.getWhiteLevel();
                    colorSlider.setWhiteLevel(oldValue + delta);
                } else if (selectedThumb == Thumb.GRAY) {
                    int oldValue = colorSlider.getGrayLevel();
                    colorSlider.setGrayLevel(oldValue + delta);
                } else { // black
                    int oldValue = slider.getValue();
                    slider.setValue(oldValue + delta);
                }
            }
        }
	        
        /**
        * Moves the selected thumb in the specified direction by a unit increment.
        * This method is called when the user presses one of the arrow keys.
        */
        public void scrollByUnit(int direction) {
            synchronized (slider) {
                int delta = 1 * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
                
                UglyColorSlider colorSlider = (UglyColorSlider) slider;
                if (selectedThumb == Thumb.WHITE) {
                    int oldValue = colorSlider.getWhiteLevel();
                    colorSlider.setWhiteLevel(oldValue + delta);
                } else if (selectedThumb == Thumb.GRAY) {
                    int oldValue = colorSlider.getGrayLevel();
                    colorSlider.setGrayLevel(oldValue + delta);
                } else { // black
                    int oldValue = slider.getValue();
                    slider.setValue(oldValue + delta);
                }
            }
        }
            
	    /**
	    * Sets the location of the upper thumb, and repaints the slider. This is
	    * called when the upper thumb is dragged to repaint the slider. The
	    * <code>setThumbLocation()</code> method performs the same task for the
	    * black thumb.
	    */
        private void setGrayThumbLocation(int x, int y) {
            Rectangle grayUnionRect = new Rectangle();
            grayUnionRect.setBounds(grayThumbRect);

            grayThumbRect.setLocation(x, y);

            SwingUtilities.computeUnion(grayThumbRect.x, grayThumbRect.y, grayThumbRect.width, grayThumbRect.height, grayUnionRect);
            slider.repaint(grayUnionRect.x, grayUnionRect.y, grayUnionRect.width, grayUnionRect.height);
        }

	    /**
 	    * Sets the location of the upper thumb, and repaints the slider. This is
 	    * called when the upper thumb is dragged to repaint the slider. The
 	    * <code>setThumbLocation()</code> method performs the same task for the
 	    * black thumb.
 	    */
         private void setWhiteThumbLocation(int x, int y) {
             Rectangle whiteUnionRect = new Rectangle();
             whiteUnionRect.setBounds(whiteThumbRect);

             whiteThumbRect.setLocation(x, y);

             SwingUtilities.computeUnion(whiteThumbRect.x, whiteThumbRect.y, whiteThumbRect.width, whiteThumbRect.height, whiteUnionRect);
             slider.repaint(whiteUnionRect.x, whiteUnionRect.y, whiteUnionRect.width, whiteUnionRect.height);
         }

         /**
         * Listener to handle model change events. This calculates the thumb
         * locations and repaints the slider if the value change is not caused by
         * dragging a thumb.
         */
         public class ChangeHandler implements ChangeListener {
        	 	@Override
             public void stateChanged(ChangeEvent arg0) {
                 if (!blackDragging && !grayDragging && !whiteDragging) {
                     calculateThumbLocation();
                     slider.repaint();
                 }
             }
         }
         
         /**
          * Listener to handle mouse movements in the slider track.
          */
         public class ColorTrackListener extends TrackListener {

        	 @Override
        	 public void mousePressed(MouseEvent e) {
        		 if (!slider.isEnabled()) {
        			 return;
        		 }

        		 currentMouseX = e.getX();
        		 currentMouseY = e.getY();

        		 if (slider.isRequestFocusEnabled()) {
        			 slider.requestFocus();
        		 }

        		 // Determine which thumb is pressed. If the upper thumb is
        		 // selected (last one dragged), then check its position first;
        		 // otherwise check the position of the black thumb first.
        		 boolean blackPressed = false;
        		 boolean grayPressed = false;
        		 boolean whitePressed = false;
        		 if (selectedThumb == Thumb.WHITE) {
        			 if (whiteThumbRect.contains(currentMouseX, currentMouseY)) {
        				 whitePressed = true;
        			 } else if (grayThumbRect.contains(currentMouseX, currentMouseY)) {
        				 grayPressed = true;
        			 } else if (blackThumbRect.contains(currentMouseX, currentMouseY)) {
        				 blackPressed = true;
        			 }
        		 } else if (selectedThumb == Thumb.GRAY) {
        			 if (grayThumbRect.contains(currentMouseX, currentMouseY)) {
        				 grayPressed = true;
        			 } else if (whiteThumbRect.contains(currentMouseX, currentMouseY)) {
        				 whitePressed = true;
        			 } else if (blackThumbRect.contains(currentMouseX, currentMouseY)) {
        				 blackPressed = true;
        			 }
        		 } else { // black selected
        			 if (blackThumbRect.contains(currentMouseX, currentMouseY)) {
        				 blackPressed = true;
        			 } else if (grayThumbRect.contains(currentMouseX, currentMouseY)) {
        				 grayPressed = true;
        			 } else if (whiteThumbRect.contains(currentMouseX, currentMouseY)) {
        				 whitePressed = true;
        			 }       			 
        		 }

        		 // Handle black thumb pressed.
        		 if (blackPressed) {
        			 switch (slider.getOrientation()) {
        			 case JSlider.VERTICAL:
        				 offset = currentMouseY - thumbRect.y;
        				 break;
        			 case JSlider.HORIZONTAL:
        				 offset = currentMouseX - thumbRect.x;
        				 break;
        			 }
        			 selectedThumb = Thumb.BLACK;
        			 blackDragging = true;
        			 return;
        		 }
        		 blackDragging = false;

        		 // Handle upper thumb pressed.
        		 if (grayPressed) {
        			 switch (slider.getOrientation()) {
        			 case JSlider.VERTICAL:
        				 offset = currentMouseY - grayThumbRect.y;
        				 break;
        			 case JSlider.HORIZONTAL:
        				 offset = currentMouseX - grayThumbRect.x;
        				 break;
        			 }
        			 selectedThumb = Thumb.GRAY;
        			 grayDragging = true;
        			 return;
        		 }
        		 grayDragging = false;
        		 
        		 // Handle upper thumb pressed.
        		 if (whitePressed) {
        			 switch (slider.getOrientation()) {
        			 case JSlider.VERTICAL:
        				 offset = currentMouseY - whiteThumbRect.y;
        				 break;
        			 case JSlider.HORIZONTAL:
        				 offset = currentMouseX - whiteThumbRect.x;
        				 break;
        			 }
        			 selectedThumb = Thumb.WHITE;
        			 whiteDragging = true;
        			 return;
        		 }
        		 whiteDragging = false;
        	 }

        	 @Override
        	 public void mouseReleased(MouseEvent e) {
        		 blackDragging = false;
        		 grayDragging = false;
        		 whiteDragging = false;
        		 slider.setValueIsAdjusting(false);
        		 super.mouseReleased(e);
        	 }

        	 @Override
        	 public void mouseDragged(MouseEvent e) {
        		 if (!slider.isEnabled()) {
        			 return;
        		 }

        		 currentMouseX = e.getX();
        		 currentMouseY = e.getY();

        		 if (blackDragging) {
        			 slider.setValueIsAdjusting(true);
        			 moveBlackThumb();

        		 } else if (whiteDragging) {
        			 slider.setValueIsAdjusting(true);
        			 moveWhiteThumb();
        		 } else if (grayDragging) {
        			 slider.setValueIsAdjusting(true);
        			 moveGrayThumb();
        		 }
        	 }

        	 @Override
        	 public boolean shouldScroll(int direction) {
        		 return false;
        	 }

        	 /**
        	  * Moves the location of the black thumb, and sets its corresponding
        	  * value in the slider.
        	  */
        	  private void moveBlackThumb() {
        		 int thumbMiddle = 0;

        		 switch (slider.getOrientation()) {
        		 case JSlider.VERTICAL:
        			 int halfThumbHeight = thumbRect.height / 2;
        			 int thumbTop = currentMouseY - offset;
        			 int trackTop = trackRect.y;
        			 int trackBottom = trackRect.y + (trackRect.height - 1);
        			 int vMax = yPositionForValue(slider.getValue() + slider.getExtent());

        			 // Apply bounds to thumb position.
        			 if (drawInverted()) {
        				 trackBottom = vMax;
        			 } else {
        				 trackTop = vMax;
        			 }
        			 thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
        			 thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

        			 setThumbLocation(thumbRect.x, thumbTop);

        			 // Update slider value.
        			 thumbMiddle = thumbTop + halfThumbHeight;
        			 slider.setValue(valueForYPosition(thumbMiddle));
        			 break;

        		 case JSlider.HORIZONTAL:
        			 int halfThumbWidth = thumbRect.width / 2;
        			 int thumbLeft = currentMouseX - offset;
        			 int trackLeft = trackRect.x;
        			 int trackRight = trackRect.x + (trackRect.width - 1);
        			 int hMax = xPositionForValue(slider.getValue() + slider.getExtent());

        			 // Apply bounds to thumb position.
        			 if (drawInverted()) {
        				 trackLeft = hMax;
        			 } else {
        				 trackRight = hMax;
        			 }
        			 thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
        			 thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

        			 setThumbLocation(thumbLeft, thumbRect.y);

        			 // Update slider value.
        			 thumbMiddle = thumbLeft + halfThumbWidth;
        			 slider.setValue(valueForXPosition(thumbMiddle));
        			 break;

        		 default:
        			 return;
        		 }
        	  }

        	  /**
        	   * Moves the location of the upper thumb, and sets its corresponding
        	   * value in the slider.
        	   */
        	  private void moveGrayThumb() {
        		  int thumbMiddle = 0;

        		  switch (slider.getOrientation()) {
        		  case JSlider.VERTICAL:
        			  int halfThumbHeight = thumbRect.height / 2;
        			  int thumbTop = currentMouseY - offset;
        			  int trackTop = trackRect.y;
        			  int trackBottom = trackRect.y + (trackRect.height - 1);
        			  int vMin = yPositionForValue(slider.getValue());

        			  // Apply bounds to thumb position.
        			  if (drawInverted()) {
        				  trackTop = vMin;
        			  } else {
        				  trackBottom = vMin;
        			  }
        			  thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
        			  thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

        			  setGrayThumbLocation(thumbRect.x, thumbTop);

        			  // Update slider extent.
        			  thumbMiddle = thumbTop + halfThumbHeight;
        			  slider.setExtent(valueForYPosition(thumbMiddle) - slider.getValue());
        			  break;

        		  case JSlider.HORIZONTAL:
        			  int halfThumbWidth = thumbRect.width / 2;
        			  int thumbLeft = currentMouseX - offset;
        			  int trackLeft = trackRect.x;
        			  int trackRight = trackRect.x + (trackRect.width - 1);
        			  int hMin = xPositionForValue(slider.getValue());

        			  // Apply bounds to thumb position.
        			  if (drawInverted()) {
        				  trackRight = hMin;
        			  } else {
        				  trackLeft = hMin;
        			  }
        			  thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
        			  thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

        			  setGrayThumbLocation(thumbLeft, thumbRect.y);

        			  // Update slider extent.
        			  thumbMiddle = thumbLeft + halfThumbWidth;
        			  slider.setExtent(valueForXPosition(thumbMiddle) - slider.getValue());
        			  break;

        		  default:
        			  return;
        		  }
        	  }
        	  /**
        	   * Moves the location of the upper thumb, and sets its corresponding
        	   * value in the slider.
        	   */
        	  private void moveWhiteThumb() {
        		  int thumbMiddle = 0;

        		  switch (slider.getOrientation()) {
        		  case JSlider.VERTICAL:
        			  int halfThumbHeight = thumbRect.height / 2;
        			  int thumbTop = currentMouseY - offset;
        			  int trackTop = trackRect.y;
        			  int trackBottom = trackRect.y + (trackRect.height - 1);
        			  int vMin = yPositionForValue(slider.getValue());

        			  // Apply bounds to thumb position.
        			  if (drawInverted()) {
        				  trackTop = vMin;
        			  } else {
        				  trackBottom = vMin;
        			  }
        			  thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
        			  thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

        			  setWhiteThumbLocation(thumbRect.x, thumbTop);

        			  // Update slider extent.
        			  thumbMiddle = thumbTop + halfThumbHeight;
        			  slider.setExtent(valueForYPosition(thumbMiddle) - slider.getValue());
        			  break;

        		  case JSlider.HORIZONTAL:
        			  int halfThumbWidth = thumbRect.width / 2;
        			  int thumbLeft = currentMouseX - offset;
        			  int trackLeft = trackRect.x;
        			  int trackRight = trackRect.x + (trackRect.width - 1);
        			  int hMin = xPositionForValue(slider.getValue());

        			  // Apply bounds to thumb position.
        			  if (drawInverted()) {
        				  trackRight = hMin;
        			  } else {
        				  trackLeft = hMin;
        			  }
        			  thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
        			  thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

        			  setWhiteThumbLocation(thumbLeft, thumbRect.y);

        			  // Update slider extent.
        			  thumbMiddle = thumbLeft + halfThumbWidth;
        			  slider.setExtent(valueForXPosition(thumbMiddle) - slider.getValue());
        			  break;

        		  default:
        			  return;
        		  }
        	  }
         }

		
	}

}
