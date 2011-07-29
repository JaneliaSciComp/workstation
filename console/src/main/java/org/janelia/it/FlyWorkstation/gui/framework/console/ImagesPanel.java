package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Self-adjusting grid of images which may be resized together.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImagesPanel extends JPanel implements Scrollable {

	private static final int MIN_THUMBNAIL_SIZE = 64;
	private static final int MAX_THUMBNAIL_SIZE = 300;
	
	private final HashMap<String, AnnotatedImageButton> buttons = new HashMap<String, AnnotatedImageButton>();
	
	private IconDemoPanel iconDemoPanel;
	private ButtonGroup buttonGroup;
	private int imageSize = MAX_THUMBNAIL_SIZE;
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
    
    public ImagesPanel(IconDemoPanel iconDemoPanel) {
        setLayout(new GridLayout(0, 2));
        setOpaque(false);
        this.iconDemoPanel = iconDemoPanel;
    }
    
    /**
     * Load in some new images asynchronously. In the meantime, show the filenames with a "loading" spinner for 
     * each image to be loaded.
     */
	public void load(List<Entity> entities, Map<Long, List<Entity>> annotationMap) {

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

        for (int i = 0; i < entities.size(); i++) {
            final Entity entity = entities.get(i);
            String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            File file = new File(iconDemoPanel.convertImagePath(filepath));

            final AnnotatedImageButton button = new AnnotatedImageButton(file.getName(), file.getAbsolutePath(), i, entity);

            button.addKeyListener(keyListener);
            
            button.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    iconDemoPanel.setCurrentEntity(entity);
                    button.setSelected(true);

                    // Scroll to the newly focused button
                    ImagesPanel.this.scrollRectToVisible(button.getBounds());
                    revalidate();
                }
            });

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount()==2 
                    		&& e.getButton()==MouseEvent.BUTTON1 
                    		&& (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                    	iconDemoPanel.showCurrentEntityDetails();
                    }
                }
            });
            
            buttons.put(entity.getId().toString(), button) ;
            buttonGroup.add(button);
            add(button);
            
            if (annotationMap != null) {
            	updateTags(annotationMap);
            }
        }
        
        for (AnnotatedImageButton button : buttons.values()) {
        	SwingWorker worker = new LoadImageWorker(button);
    		worker.execute();
    		workers.add(worker);
        }
    }

	public void updateTags(Map<Long, List<Entity>> annotationMap) {
		
        for (final AnnotatedImageButton button : buttons.values()) {
        	
        	final Entity entity = button.getEntity();
	        TagCloudPanel<Entity> tagPanel = button.getTagPanel();
	        tagPanel.setTags(annotationMap.get(entity.getId()));
			
	        for(Entry<Entity,JLabel> entry : tagPanel.getTagLabels().entrySet()) {
	        	
	        	final Entity annotation = entry.getKey();
				
	        	entry.getValue().addMouseListener(new MouseAdapter() {
	                @Override
	                public void mouseClicked(MouseEvent e) {
	                    if (e.getClickCount()==2) {
	                    	Utils.setWaitingCursor(iconDemoPanel);
	                        ModelMgr.getModelMgr().deleteAnnotation((String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME),
	                        		button.getEntity().getId(),annotation.getName());
	                        SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().refreshEntity(entity);
	                        updateUI();	
	                    }
	                }
	            });
	        }
        }
        
        updateUI();
	}
	
	/**
	 * Scale all the images to the desired percent of their true size. 
	 * @param imageSizePercent
	 */
    public void rescaleImages(double imageSizePercent) {
    	if (imageSizePercent < 0 || imageSizePercent > 1) {
    		return;
    	}
    	double range = (double)(MAX_THUMBNAIL_SIZE - MIN_THUMBNAIL_SIZE);
    	rescaleImages(MIN_THUMBNAIL_SIZE + (int)(range*imageSizePercent));
	}
    
	/**
	 * Scale all the images to the given max size. 
	 * @param imageSize
	 */
    public void rescaleImages(int imageSize) {
    	if (imageSize < MIN_THUMBNAIL_SIZE || imageSize > MAX_THUMBNAIL_SIZE || imageSize == this.imageSize) {
    		return;
    	}
		this.imageSize = imageSize;
        for (AnnotatedImageButton button : buttons.values()) {
    		button.rescaleImage(imageSize);
        }
	}
    
    /**
     * Returns the currently selected image button in the panel.
     * @return
     */
    public AnnotatedImageButton getSelectedImage() {
        if (iconDemoPanel.getCurrentEntity() == null) {
        	return null;
        }
        return buttons.get(iconDemoPanel.getCurrentEntity());
    }

	public void setSelectedImage(Entity entity) {
		AnnotatedImageButton button = buttons.get(entity.getId().toString());
		button.setSelected(true);
	}
	
    /**
     * Set the number of columns in the grid layout based on the width of the parent component and the width of the
     * buttons.
     */
    public void recalculateGrid() {
		double maxButtonWidth = 0;
        for (AnnotatedImageButton button : buttons.values()) {
    		int w = button.getPreferredSize().width;
    		if (w>maxButtonWidth) maxButtonWidth = w;
        }
        
        // Should not be needed, but just in case, lets make sure we never divide by zero
        if (maxButtonWidth == 0) maxButtonWidth = 400;
        
    	int numCols = (int)Math.floor((double)getParent().getSize().width / maxButtonWidth);
    	if (numCols > 0) {    		
    		((GridLayout)getLayout()).setColumns(numCols);
    	}

        revalidate();
        repaint();
    }
    
    /**
     * Add or remove the given tag from the currently selected image button.
     * @param tag
     */
//    public boolean addOrRemoveTag(String tag) {
//        if (iconDemoPanel.getCurrentEntity() == null) {
//        	throw new IllegalStateException("Cannot add a tag when there is no button selected");
//        }
//        AnnotatedImageButton currButton = buttons.get(iconDemoPanel.getCurrentEntity());
//        boolean added = currButton.addOrRemoveTag(tag);
//        return added;
//    }
 
    public HashMap<String, AnnotatedImageButton> getButtons() {
		return buttons;
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
            	button.loadImage(MAX_THUMBNAIL_SIZE);
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
            	if (button.getDisplaySize() != imageSize) {
            		button.rescaleImage(imageSize);
            	}
            }
        	recalculateGrid();
        }
        
    }
}