package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
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

	public static final int MIN_TABLE_HEIGHT = 50;
	public static final int DEFAULT_TABLE_HEIGHT = 200;
	public static final int MAX_TABLE_HEIGHT = 500;
	
    private final HashMap<String, AnnotatedImageButton> buttons = new LinkedHashMap<String, AnnotatedImageButton>();
    
    private KeyListener buttonKeyListener;
    private MouseListener buttonMouseListener;
    
    private final IconDemoPanel iconDemoPanel;
    private ScrollableGridPanel buttonsPanel;

    private Integer maxImageHeight;
    private Integer currImageSize = DEFAULT_THUMBNAIL_SIZE;
    private Integer currTableHeight = DEFAULT_TABLE_HEIGHT;
    
    private Rectangle currViewRect;
    
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
    
    public ImagesPanel(IconDemoPanel iconDemoPanel) {
    	this.iconDemoPanel = iconDemoPanel;
    	buttonsPanel = new ScrollableGridPanel();
        setViewportView(buttonsPanel);
        setBorder(BorderFactory.createEmptyBorder());
    }

	/**
     * Returns the button with the given entity.
     *
     * @return
     */
    public AnnotatedImageButton getButtonById(String id) {
        return buttons.get(id);
    }
    
    public void setButtonKeyListener(KeyListener buttonKeyListener) {
		this.buttonKeyListener = buttonKeyListener;
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
    public void setRootedEntities(List<RootedEntity> rootedEntities) {
    	
        buttons.clear();
        for (Component component : buttonsPanel.getComponents()) {
            if (component instanceof AnnotatedImageButton) {
            	AnnotatedImageButton button = (AnnotatedImageButton)component;
            	buttonsPanel.remove(button);
            }
        }

		ImageCache imageCache = SessionMgr.getBrowser().getImageCache();
			
        for (int i = 0; i < rootedEntities.size(); i++) {
            final RootedEntity rootedEntity = rootedEntities.get(i);
            
            if (buttons.containsKey(rootedEntity.getId())) continue;
            
            AnnotatedImageButton button = null;

            String filepath = EntityUtils.getImageFilePath(rootedEntity.getEntity(), iconDemoPanel.getCurrImageRole());
            if (filepath != null) {
            	button = new DynamicImageButton(rootedEntity, iconDemoPanel);
                ((DynamicImageButton)button).setCache(imageCache);
            }
            else {
            	button = new StaticImageButton(rootedEntity, iconDemoPanel);
            }
            
            button.setTitleVisible(iconDemoPanel.areTitlesVisible());
            button.setTagsVisible(iconDemoPanel.areTagsVisible());
            
            if (buttonKeyListener!=null) button.addKeyListener(buttonKeyListener);
            if (buttonMouseListener!=null) button.addMouseListener(buttonMouseListener);
            
            button.addMouseListener(new MouseForwarder(this, "AnnotatedImageButton->ImagesPanel"));
            
            // Disable tab traversal, we will do it ourselves
            button.setFocusTraversalKeysEnabled(false);
            
            buttons.put(rootedEntity.getId(), button);
            buttonsPanel.add(button);
        }
        
        resetMaxImageHeight();
    }
    
    public void removeRootedEntity(RootedEntity rootedEntity) {
    	AnnotatedImageButton button = buttons.get(rootedEntity.getId());
    	buttonsPanel.remove(button);
    	buttons.remove(rootedEntity.getId());
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(Annotations annotations) {
        for (AnnotatedImageButton button : buttons.values()) {
        	loadAnnotations(annotations, button.getRootedEntity().getEntity().getId());
        }
    }

    /**
     * Show the given annotations on the appropriate images.
     */
    public void loadAnnotations(final Annotations annotations, final Long entityId) {
    	SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		    	for (AnnotatedImageButton button : getButtonsByEntityId(entityId)) {
		        	List<OntologyAnnotation> entityAnnotations = annotations.getFilteredAnnotationMap().get(entityId);
		            button.showAnnotations(entityAnnotations);
		    	}
			}
		});
    }

    public List<AnnotatedImageButton> getButtonsByEntityId(Long entityId) {
    	List<AnnotatedImageButton> entityButtons = new ArrayList<AnnotatedImageButton>();
        for (AnnotatedImageButton button : buttons.values()) {
        	if (button.getRootedEntity().getEntity().getId().equals(entityId)) {
        		entityButtons.add(button);
        	}
        }
        return entityButtons;
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
        if (currImageSize > imageSize) {
        	resetMaxImageHeight();
        }
        this.currImageSize = imageSize;
        for (AnnotatedImageButton button : buttons.values()) {
        	try {
        		int imageHeight = getMaxImageHeight()==null? imageSize : getMaxImageHeight();
                button.rescaleImage(imageSize, imageHeight);
	    	}
	    	catch (Exception e) {
	    		SessionMgr.getSessionMgr().handleException(e);
	    	}
        }
    }

    public synchronized void resizeTables(int tableHeight) {
    	if (tableHeight < MIN_TABLE_HEIGHT || tableHeight > MAX_TABLE_HEIGHT) {
    		return;
    	}
        this.currTableHeight = tableHeight;
    	for (AnnotatedImageButton button : buttons.values()) {
        	try {
                button.resizeTable(tableHeight);
	    	}
	    	catch (Exception e) {
	    		SessionMgr.getSessionMgr().handleException(e);
	    	}
        }
    }

    public synchronized void showAllButtons() {
    	buttonsPanel.removeAll();
    	for(AnnotatedImageButton button : buttons.values()) {
    		buttonsPanel.add(button);
    	}
    	revalidate();
    	repaint();
    }
    
    public synchronized void hideButtons(Collection<Long> entityIds) {
    	for(Long entityId : entityIds) {
    		AnnotatedImageButton button = getButtonById(entityId+"");
    		buttonsPanel.remove(button);
    	}
    	revalidate();
    	repaint();
    }
    
    public int getCurrImageSize() {
		return currImageSize;
	}
    
    public int getCurrTableHeight() {
		return currTableHeight;
	}

	public void scrollToBottom() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			    getViewport().scrollRectToVisible(new Rectangle(0, buttonsPanel.getHeight(), 1, 1));
			}
		});
	}
	
	public void scrollEntityToCenter(RootedEntity rootedEntity) {
		if (rootedEntity == null) return;
    	AnnotatedImageButton selectedButton = getButtonById(rootedEntity.getId());
    	scrollButtonToCenter(selectedButton);
	}
	
	public void scrollButtonToVisible(AnnotatedImageButton button) {
    	if (button == null) return;
	    getViewport().scrollRectToVisible(button.getBounds());
	}
	
	public void scrollButtonToCenter(AnnotatedImageButton button) {

    	if (button == null) return;
	    JViewport viewport = getViewport();
    	
	    // This rectangle is relative to the table where the
	    // northwest corner of cell (0,0) is always (0,0).
	    Rectangle rect = button.getBounds();

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

	public void scrollButtonToTop(AnnotatedImageButton button) {

    	if (button == null) return;
	    JViewport viewport = getViewport();
    	
	    // This rectangle is relative to the table where the
	    // northwest corner of cell (0,0) is always (0,0).
	    Rectangle rect = button.getBounds();

	    // The location of the view relative to the table
	    Rectangle viewRect = viewport.getViewRect();

	    // Translate the cell location so that it is relative
	    // to the view, assuming the northwest corner of the
	    // view is (0,0). Also make the rect as large as the view, 
	    // so that the relevant portion goes to the top.
	    rect.setBounds(rect.x-viewRect.x, rect.y-viewRect.y, viewRect.width, viewRect.height);

	    // Scroll the area into view.
	    viewport.scrollRectToVisible(rect);
	}
	
	public void scrollSelectedEntitiesToCenter() {
		List<AnnotatedImageButton> selected = getSelectedButtons();
		if (selected.isEmpty()) return;
		int i = selected.size()/2;
		AnnotatedImageButton centerOfMass = selected.get(i);
		scrollButtonToCenter(centerOfMass);
	}

	public void scrollSelectedEntitiesToTop() {
		List<AnnotatedImageButton> selected = getSelectedButtons();
		if (selected.isEmpty()) return;
		scrollButtonToTop(selected.get(0));
	}
	
    public synchronized Integer getMaxImageHeight() {
		return maxImageHeight;
	}

    public synchronized void resetMaxImageHeight() {
    	this.maxImageHeight = null;
    }
    
	public synchronized void registerImageHeight(Integer imageHeight) {
		if (maxImageHeight==null || imageHeight>maxImageHeight) {
			this.maxImageHeight = imageHeight;	
		}
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

    public void setTagTable(boolean tagTable) {
        for (final AnnotatedImageButton button : buttons.values()) {
        	if (tagTable) {
        		if (button.getAnnotationView() instanceof AnnotationTablePanel) return;
        		button.setAnnotationView(new AnnotationTablePanel());
        	}
        	else {
        		if (button.getAnnotationView() instanceof AnnotationTagCloudPanel) return;
        		button.setAnnotationView(new AnnotationTagCloudPanel() {
        			@Override
        			protected void moreDoubleClicked(MouseEvent e) {
        		        new EntityDetailsDialog().showForRootedEntity(button.getRootedEntity());
        			}
        		});
        	}
        }
    }
    
    public void setInvertedColors(boolean inverted) {
        for (AnnotatedImageButton button : buttons.values()) {
            button.setInvertedColors(inverted);
        }
    }

    private boolean setSelection(final AnnotatedImageButton button, boolean selection) {
    	if (button.isSelected()!=selection) {
    		button.setSelected(selection);
    		return true;
    	}
    	return false;
    }
    
    /**
     * The identifier can be either a uniqueId (path) or just a simple entity id. In the latter case you may get 
     * multiple entities selected, if there are duplicates.
     * @param selectedEntityId
     * @param selection
     * @param clearAll
     */
    public void setSelection(String selectedEntityId, boolean selection, boolean clearAll) {
    	if (clearAll) {
			for(AnnotatedImageButton button : buttons.values()) {
				RootedEntity rootedEntity = button.getRootedEntity();
				if (rootedEntity.getId().equals(selectedEntityId) || rootedEntity.getEntity().getId().toString().equals(selectedEntityId)) {
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
    
    public List<AnnotatedImageButton> getSelectedButtons() {
    	List<AnnotatedImageButton> selected = new ArrayList<AnnotatedImageButton>();
		for(AnnotatedImageButton button : buttons.values()) {
			if (button.isSelected()) {
				selected.add(button);
			}
		}
		return selected;
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
        
        int numCols = (int) Math.floor((double)fullWidth / maxButtonWidth);
        if (numCols > 0) {
        	buttonsPanel.setColumns(numCols);
        }

        buttonsPanel.revalidate();
        buttonsPanel.repaint();
    }

    public synchronized void loadUnloadImages() {
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		    	final JViewport viewPort = getViewport();
		    	Rectangle viewRect = viewPort.getViewRect();
		    	if (buttonsPanel.getColumns() == 1) {
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
			setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
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
        
        public int getColumns() {
        	return ((GridLayout)getLayout()).getColumns();
        }
        
        public void setColumns(int columns) {
        	((GridLayout)getLayout()).setColumns(columns);
        }
    }
}