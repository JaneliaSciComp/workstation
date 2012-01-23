package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Self-adjusting grid of images which may be resized together.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImagesPanel extends JScrollPane {

    public static final int MIN_THUMBNAIL_SIZE = 100;
    public static final int DEFAULT_THUMBNAIL_SIZE = 300;
    public static final int MAX_THUMBNAIL_SIZE = 1000;

    private final HashMap<String, AnnotatedImageButton> buttons = new HashMap<String, AnnotatedImageButton>();

    private final ImageCache imageCache = new ImageCache();
    
    private KeyListener buttonKeyListener;
    private FocusListener buttonFocusListener;
    private MouseListener buttonMouseListener;
    
    private JPanel buttonsPanel;

    private int currImageSize = DEFAULT_THUMBNAIL_SIZE;
    private Rectangle currViewRect;
	private int numCols;
    
    // Listen for scroll events
    private final AdjustmentListener scrollListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(final AdjustmentEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
    			@Override
    			public void run() {
    		    	final JViewport viewPort = getViewport();
    		    	Rectangle viewRect = viewPort.getViewRect();
    		    	if (viewRect.equals(currViewRect)) {
    		    		return;
    		    	}
    		    	currViewRect = viewRect;
    	        	loadUnloadImages();
    			}
    		});
        }
    };
    
    public ImagesPanel() {
    	buttonsPanel = new ScrollableGridPanel();
        setViewportView(buttonsPanel);
    }


	/**
     * Returns the button with the given entity.
     *
     * @return
     */
    public AnnotatedImageButton getButtonByEntityId(long entityId) {
        return buttons.get(entityId+"");
    }
    
    /**
     * TODO: remove this after refactoring so that its not needed.
     */
    public HashMap<String, AnnotatedImageButton> getButtons() {
        return buttons;
    }
    
    public void setButtonKeyListener(KeyListener buttonKeyListener) {
		this.buttonKeyListener = buttonKeyListener;
	}

	public void setButtonFocusListener(FocusListener buttonFocusListener) {
		this.buttonFocusListener = buttonFocusListener;
	}
	
	public void setButtonMouseListener(MouseListener buttonMouseListener) {
		this.buttonMouseListener = buttonMouseListener;
	}

	public void setScrollLoadingEnabled(boolean enabled) {
		if (enabled) {
			// Reset scrollbar and re-add the listener
	        SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					getVerticalScrollBar().setValue(0); 
			        getVerticalScrollBar().addAdjustmentListener(scrollListener);
				}
			});
		}
		else {
	    	// Remove the scroll listener so that we don't get a bunch of bogus events as things are added to the imagesPanel
	    	getVerticalScrollBar().removeAdjustmentListener(scrollListener);
		}
	}
	
	public void cancelAllLoads() {
		for(AnnotatedImageButton button : buttons.values()) {
        	if (button instanceof DynamicImageButton) {
        		((DynamicImageButton)button).cancelLoad();
        	}
        }
	}
	
	/**
     * Create the image buttons, but leave the images unloaded for now.
     */
    public void setEntities(List<Entity> entities) {
    	
        buttons.clear();
        for (Component component : buttonsPanel.getComponents()) {
            if (component instanceof AnnotatedImageButton) {
            	AnnotatedImageButton button = (AnnotatedImageButton)component;
//            	if (button instanceof DynamicImageButton) {
//            		((DynamicImageButton)button).cancelLoad();
//            	}
            	buttonsPanel.remove(button);
            }
        }

		IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
		
        for (int i = 0; i < entities.size(); i++) {
            final Entity entity = entities.get(i);
            
            if (buttons.containsKey(entity.getId().toString())) continue;
            
            AnnotatedImageButton button = null;
            String filepath = EntityUtils.getDefaultImageFilePath(entity);
            if (filepath != null) {
            	button = new DynamicImageButton(entity);
                ((DynamicImageButton)button).setCache(imageCache);
            }
            else {
            	button = new StaticImageButton(entity);
            }
            
            button.setTitleVisible(iconDemoPanel.areTitlesVisible());
            button.setTagsVisible(iconDemoPanel.areTagsVisible());
            
            if (buttonKeyListener!=null) button.addKeyListener(buttonKeyListener);
            if (buttonFocusListener!=null) button.addFocusListener(buttonFocusListener);
            if (buttonMouseListener!=null) button.addMouseListener(buttonMouseListener);
            
            // Disable tab traversal, we will do it ourselves
            button.setFocusTraversalKeysEnabled(false);
            
            buttons.put(entity.getId().toString(), button);
            buttonsPanel.add(button);
        }
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations) {
        for (AnnotatedImageButton button : buttons.values()) {
        	loadAnnotations(annotations, button.getEntity());
        }
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations, Entity entity) {
    	AnnotatedImageButton button = getButtonByEntityId(entity.getId());
    	List<OntologyAnnotation> entityAnnotations = annotations.getFilteredAnnotationMap().get(entity.getId());
        button.getTagPanel().setTags(entityAnnotations);
    }

    /**
     * Scale all the images to the given max size.
     *
     * @param imageSize
     */
    public synchronized void rescaleImages(int imageSize) {
        if (imageSize < MIN_THUMBNAIL_SIZE || imageSize > MAX_THUMBNAIL_SIZE) {
            return;
        }
        this.currImageSize = imageSize;
        for (AnnotatedImageButton button : buttons.values()) {
        	try {
                button.rescaleImage(imageSize);
	    	}
	    	catch (Exception e) {
	    		SessionMgr.getSessionMgr().handleException(e);
	    	}
        }
    }

    
    public int getCurrImageSize() {
		return currImageSize;
	}
	
	public void scrollEntityToCenter(Entity entity) {
	    
		if (entity == null) return;
		
	    JViewport viewport = getViewport();
    	AnnotatedImageButton selectedButton = getButtonByEntityId(entity.getId());
    	if (selectedButton == null) return;
    	
	    // This rectangle is relative to the table where the
	    // northwest corner of cell (0,0) is always (0,0).
	    Rectangle rect = selectedButton.getBounds();

	    // The location of the view relative to the table
	    Rectangle viewRect = viewport.getViewRect();

	    // Translate the cell location so that it is relative
	    // to the view, assuming the northwest corner of the
	    // view is (0,0).
	    rect.setLocation(rect.x-viewRect.x, rect.y-viewRect.y);

	    // Calculate location of rect if it were at the center of view
	    int centerX = (viewRect.width-rect.width)/2;
	    int centerY = (viewRect.height-rect.height)/2;

	    // Fake the location of the cell so that scrollRectToVisible
	    // will move the cell to the center
	    if (rect.x < centerX) {
	        centerX = -centerX;
	    }
	    if (rect.y < centerY) {
	        centerY = -centerY;
	    }
	    rect.translate(centerX, centerY);

	    // Scroll the area into view.
	    viewport.scrollRectToVisible(rect);
	}
    
    public void setTitleVisbility(boolean visible) {
        for (AnnotatedImageButton button : buttons.values()) {
            button.setTitleVisible(visible);
        }
    }

    public void setTagVisbility(boolean visible) {
        for (AnnotatedImageButton button : buttons.values()) {
            button.setTagsVisible(visible);
        }
    }

    public void setInvertedColors(boolean inverted) {
        for (AnnotatedImageButton button : buttons.values()) {
            button.setInvertedColors(inverted);
        }
    }

    private boolean setSelection(AnnotatedImageButton button, boolean selection) {
    	if (button.isSelected()!=selection) {
    		button.setSelected(selection);
			if (selection) button.requestFocus();
    		return true;
    	}
    	return false;
    }
    
    public void setSelection(Long selectedEntityId, boolean selection, boolean clearAll) {
    	if (clearAll) {
			for(AnnotatedImageButton button : buttons.values()) {
				if (button.getEntity().getId().equals(selectedEntityId)) {
					setSelection(button, true);
				}
				else {
					setSelection(button, false);
				}
			}
    	}
	    else {
	        AnnotatedImageButton button = buttons.get(selectedEntityId.toString());
	        if (button != null) {
	        	setSelection(button, selection);
	        }
	    }
	}
    
    /**
     * Set the number of columns in the grid layout based on the width of the parent component and the width of the
     * buttons.
     */
    public synchronized void recalculateGrid() {
    	
    	if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("recalculateGrid called outside of EDT");
    	
    	double maxButtonWidth = 0;
        for (AnnotatedImageButton button : buttons.values()) {
            int w = button.getPreferredSize().width;
            if (w > maxButtonWidth) maxButtonWidth = w;
        }

        // Should not be needed, but just in case, lets make sure we never divide by zero
        if (maxButtonWidth == 0) maxButtonWidth = 400;
        
        int fullWidth = getSize().width - getVerticalScrollBar().getWidth();
        this.numCols = (int) Math.floor((double)fullWidth / maxButtonWidth);
        if (numCols > 0) {
            ((GridLayout)buttonsPanel.getLayout()).setColumns(numCols);
        }
    }

    public synchronized void loadUnloadImages() {
    	
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		    	final JViewport viewPort = getViewport();
		    	Rectangle viewRect = viewPort.getViewRect();
		    	
		    	if (numCols == 1) {
		    		viewRect.setSize(viewRect.width, viewRect.height+100);
		    	}
		    	
		        for(AnnotatedImageButton button : buttons.values()) {
		        	try {
		        		button.setViewable(viewRect.intersects(button.getBounds()));
		        	}
		        	catch (Exception e) {
		        		SessionMgr.getSessionMgr().handleException(e);
		        	}
		        }
			}
		});
    }
    
    private class ScrollableGridPanel extends JPanel implements Scrollable  {

		public ScrollableGridPanel() {
			setLayout(new GridLayout(0, 2));
			setOpaque(false);
		}
		
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 300;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}