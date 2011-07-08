package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;

/**
 * Self-adjusting panel of images
 */
public class ImagesPanel extends JPanel implements Scrollable {
	
	private final List<AnnotatedImageButton> buttons = new ArrayList<AnnotatedImageButton>();
	
	private ButtonGroup buttonGroup;
    private double imageSizePercent = 1.0d;
    private Integer currIndex;
    private List<SwingWorker> workers = new ArrayList<SwingWorker>();
    
    // Listen for key strokes and execute the appropriate key bindings
    private KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (KeymapUtil.isModifier(e)) return;
                KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
                ConsoleApp.getKeyBindings().executeBinding(shortcut);
            }
        }
    };
    
    public ImagesPanel() {
        setLayout(new GridLayout(0, 2));
        setOpaque(false);
    }
    
    /**
     * Load in some new images asynchronously. In the meantime, show the filenames with a "loading" spinner for 
     * each image to be loaded.
     * @param labels
     * @param filenames
     */
	public void load(List<String> labels, List<String> filenames) {

		for(SwingWorker worker : workers) {
	    	if (worker != null && !worker.isDone()) {
	    		worker.cancel(true);	
	    	}	
		}
    	
    	((GridLayout)getLayout()).setColumns(10);
    	
        buttons.clear();
        buttonGroup = new ButtonGroup();
        for (Component component : getComponents()) {
        	if (component instanceof AnnotatedImageButton) {
        		remove(component);
        	}
        }
    	
        for(int i=0; i<labels.size(); i++) {
        	
        	final AnnotatedImageButton button = new AnnotatedImageButton(labels.get(i), filenames.get(i), i);
        	final int index = i;
        	
        	button.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    currIndex = index;
                    button.setSelected(true);
                    
                    // Scroll to the newly focused button
                    ImagesPanel.this.scrollRectToVisible(button.getBounds());
                    SwingUtilities.updateComponentTreeUI(ImagesPanel.this.getParent());
                }
            });

        	button.addKeyListener(keyListener);

        	buttons.add(button);
            buttonGroup.add(button);
        	add(button);
        }

        for (AnnotatedImageButton button : buttons) {
        	SwingWorker worker = new LoadImageWorker(button);
    		worker.execute();
    		workers.add(worker);
        }
    }
    
	/**
	 * Scale all the images to the desired percent of their true size. Also recalculates maxButtonWidth so that the 
	 * grid may be recalculated by recalculateGrid.
	 * @param imageSizePercent
	 */
    public void rescaleImages(double imageSizePercent) {
    	if (imageSizePercent < 0 || imageSizePercent == this.imageSizePercent) {
    		return;
    	}
		this.imageSizePercent = imageSizePercent;
        for (AnnotatedImageButton button : buttons) {
    		button.rescaleImage(imageSizePercent);
        }
	}
    
    /**
     * Returns the currently selected image button in the panel.
     * @return
     */
    public AnnotatedImageButton getSelectedImage() {
        if (currIndex == null || currIndex >= buttons.size()) {
        	return null;
        }
        return buttons.get(currIndex);
    }

    /**
     * Set the number of columns in the grid layout based on the width of the parent component and the width of the
     * buttons.
     */
    public void recalculateGrid() {
		double maxButtonWidth = 0;
        for (AnnotatedImageButton button : buttons) {
    		int w = button.getPreferredSize().width;
    		if (w>maxButtonWidth) maxButtonWidth = w;
        }
        
        // Should not be needed, but just in case, lets make sure we never divide by zero
        if (maxButtonWidth == 0) maxButtonWidth = 400;
        
    	int numCols = (int)Math.floor((double)getParent().getSize().width / maxButtonWidth);
    	if (numCols > 0) {    		
    		((GridLayout)getLayout()).setColumns(numCols);
    	}
    	
        SwingUtilities.updateComponentTreeUI(this);
    }

    /**
     * Add or remove the given tag from the currently selected image button.
     * @param tag
     */
    public boolean addOrRemoveTag(String tag) {
        if (currIndex == null || currIndex >= buttons.size()) {
        	throw new IllegalStateException("Cannot add a tag when there is no button selected");
        }
        AnnotatedImageButton currButton = buttons.get(currIndex);
        boolean added = currButton.addOrRemoveTag(tag);
        SwingUtilities.updateComponentTreeUI(ImagesPanel.this);
        return added;
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * SwingWorker class that loads the images and rescales them to the current imageSizePercent sizing.  This 
     * thread supports being canceled.
     *
     */
    private class LoadImageWorker extends SwingWorker<Void, AnnotatedImageButton> {
        
    	private AnnotatedImageButton button;
    	
        public LoadImageWorker(AnnotatedImageButton button) {
			this.button = button;
		}

		@Override
        protected Void doInBackground() throws Exception {
        	try {
            	if (isCancelled()) return null;
            	button.loadImage(imageSizePercent);
            	if (isCancelled()) return null;
                publish(button);
                if (isCancelled()) return null;
        	}
        	catch (Throwable e) {
        		e.printStackTrace();
        	}
            return null;
        }

        @Override
        protected void process(List<AnnotatedImageButton> buttons) {
        	// If the scale has changed since the image began loading then we have to rescale it
            for (AnnotatedImageButton button : buttons) {
            	if (button.getScale() != imageSizePercent)
            		button.rescaleImage(imageSizePercent);
            		
            }
        	recalculateGrid();
        }
    }
}