package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.LRUCache;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;


/**
 * A lazy-loading image with a title on top and optional annotation tags underneath.
 */
public class AnnotatedImageButton extends JToggleButton {

	private static final Map<String, BufferedImage> imageCache = 
		Collections.synchronizedMap(new LRUCache<String, BufferedImage>(50));
	
    private final Entity entity;
    private final JTextPane imageCaption;
    private final AnnotationTagCloudPanel tagPanel;
    
    private final JPanel buttonPanel;
    private final JPanel imagePanel;
    private final JLabel loadingLabel;
    private final JLabel imageLabel;
    private final JLabel errorLabel;
    
    private final String title;
    private final String imageFilename;
    private BufferedImage maxSizeImage;
    private BufferedImage invertedMaxSizeImage;
    private int displaySize;
    private boolean inverted = false;
    private boolean viewable = false;
    private LoadImageWorker loadWorker;
    
    public AnnotatedImageButton(String title, String imageFilename, final int index, final Entity entity) {
        this.entity = entity;
        this.title = title;
        this.imageFilename = imageFilename;

        GridBagConstraints c = new GridBagConstraints();
        buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        add(buttonPanel);
    	
        imageCaption = new JTextPane();
        imageCaption.setFocusable(false);
        imageCaption.setText(title);
        imageCaption.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        imageCaption.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageCaption.setEditable(false);
        imageCaption.setOpaque(false);
        StyledDocument doc = imageCaption.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(imageCaption, c);

        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        errorLabel = new JLabel();
        errorLabel.setOpaque(false);
        errorLabel.setForeground(Color.red);
        errorLabel.setIcon(Icons.getMissingIcon());
        errorLabel.setVerticalTextPosition(JLabel.BOTTOM);
        errorLabel.setHorizontalTextPosition(JLabel.CENTER);
        
        imageLabel = new JLabel();
        imageLabel.setOpaque(false);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setPreferredSize(new Dimension(ImagesPanel.MAX_THUMBNAIL_SIZE, ImagesPanel.MAX_THUMBNAIL_SIZE));
        imagePanel.setOpaque(false);
        setImageLabel(loadingLabel);
        
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(imagePanel, c);

        tagPanel = new AnnotationTagCloudPanel();

        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add(tagPanel, c);


        // Fix event dispatching so that user can click on the title or the tags and still select the button

        imageCaption.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AnnotatedImageButton.this.dispatchEvent(e);
            }
        });
        
        // Mouse events
        
        this.addMouseListener(new MouseHandler() {

			@Override
			protected void popupTriggered(MouseEvent e) {
				
				IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
                iconDemoPanel.setCurrentEntity(entity);
                
	            JPopupMenu popupMenu = new JPopupMenu();
	            
	            JMenuItem titleMenuItem = new JMenuItem(entity.getName());
	            titleMenuItem.setEnabled(false);
	            popupMenu.add(titleMenuItem);
	            
	            JMenuItem v3dMenuItem = new JMenuItem("  View in V3D (Neuron Annotator)");
	            v3dMenuItem.addActionListener(new ActionListener() {
	                public void actionPerformed(ActionEvent actionEvent) {
	    				try {
	    					Entity result = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
		                    if (result != null && ModelMgr.getModelMgr().notifyEntityViewRequestedInNeuronAnnotator(result.getId())) {
		                    	// Success
		                    	return;
		                    }
	    				} 
	    				catch (Exception e) {
	    					e.printStackTrace();
	    				}
	                }
	            });
	            popupMenu.add(v3dMenuItem);
		        
		        JMenuItem detailsMenuItem = new JMenuItem("  View details");
	            detailsMenuItem.addActionListener(new ActionListener() {
	                public void actionPerformed(ActionEvent actionEvent) {
	    				IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
	                    iconDemoPanel.setCurrentEntity(entity);
	                    iconDemoPanel.showCurrentEntityDetails();
	                }
	            });
	            popupMenu.add(detailsMenuItem);
	            
		        popupMenu.show(AnnotatedImageButton.this, e.getX(), e.getY());
			}

			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
                iconDemoPanel.setCurrentEntity(entity);
                iconDemoPanel.showCurrentEntityDetails();
			}
        	
        });

    }

    public synchronized void setTitleVisible(boolean visible) {
        imageCaption.setVisible(visible);
		invalidate();
    }

    public synchronized void setTagsVisible(boolean visible) {
        tagPanel.setVisible(visible);
		invalidate();
    }
    
    public synchronized void rescaleImage(int imageSize) {
    	if (viewable) {
    		if (maxSizeImage == null) {
    			// Must be currently loading, in which case this method will get called again when the loading is done
    		}
    		else {
	    		if (inverted && invertedMaxSizeImage == null) {
	    			setInvertedColors(true);
	    		}
	            BufferedImage image = Utils.getScaledImageIcon(inverted ? invertedMaxSizeImage : maxSizeImage, imageSize);
	            imageLabel.setIcon(new ImageIcon(image));
    		}
    	}
    	else {
    		if (maxSizeImage != null) {
				System.out.println("WARNING: nonviewable image has a non-null maxSizeImage in memory");
    		}
    		
    		if (imagePanel.getComponentCount() != 1) {
    			System.out.println("Warning: image panel has "+imagePanel.getComponentCount()+" components "+entity.getName());
    		}
    		else {
	    		if (imagePanel.getComponents()[0] != loadingLabel && imagePanel.getComponents()[0] != errorLabel) {
	    			System.out.println("Warning: non-viewable image has a non loading label "+entity.getName());
	    			if (imagePanel.getComponents()[0] == imageLabel) {
	    				System.out.println("it's an image label!");
	    			}
	    			else if (imagePanel.getComponents()[0] == null) {
	    				System.out.println("it's null!");
	    			}
	    			else {
	    				System.out.println("WTF "+imagePanel.getComponents()[0]);
	    			}
	    		}
    		}
    		
    		// Just in case.. this shouldn't happen, but very rarely it does due to some synchronization issue I can't track down.
    		setImageLabel(loadingLabel);
    	}
    	
		imagePanel.setPreferredSize(new Dimension(imageSize, imageSize));
        this.displaySize = imageSize;
        
		revalidate();
		repaint();
    }

    public synchronized void setInvertedColors(boolean inverted) {

        this.inverted = inverted;
        if (inverted == true && maxSizeImage != null) {
            invertedMaxSizeImage = Utils.invertImage(maxSizeImage);
        }
        else {
            // Free up memory when we don't need inverted images
            invertedMaxSizeImage = null;
        }

        rescaleImage(displaySize);
    }

    /**
     * Tell the button if its image should be viewable. When this is set to false, the images can be released from
     * memory to save space. When it's set to true, the image will be reloaded from disk if necessary.
     * @param wantViewable
     */
	public synchronized void setViewable(boolean wantViewable) {

		if (wantViewable) {
			if (!this.viewable) {
				loadWorker = new LoadImageWorker();
				loadWorker.execute();
			}
		}
		else {
			if (loadWorker != null && !loadWorker.isDone()) {
				loadWorker.cancel(true);
				loadWorker = null;
			}
	    	// Clear all references to the image data so that it can be cleared out of memory
	    	maxSizeImage = null;
	    	invertedMaxSizeImage = null;
	    	imageLabel.setIcon(null);
	    	// Show the loading label until the image needs to be loaded again
	        setImageLabel(loadingLabel);
			invalidate();
		}
		this.viewable = wantViewable;
	}
	
	/**
	 * Returns the thread that is loading this image, or null if it's not being loaded. This is useful for 
	 * canceling the image load using the cancel() method.
	 * @return
	 */
    public SimpleWorker getLoadWorker() {
		return loadWorker;
	}
    
	/**
     * SwingWorker class that loads the image and rescales it to the current imageSizePercent sizing.  This
     * thread supports being canceled.
     */
    private class LoadImageWorker extends SimpleWorker {
    	
        @Override
		protected void doStuff() throws Exception {
            int size = ImagesPanel.MAX_THUMBNAIL_SIZE;
            
            BufferedImage maxSizeImage = imageCache.containsKey(imageFilename) ? 
            		imageCache.get(imageFilename) : 
        			Utils.getScaledImageIcon(Utils.readImage(imageFilename), size);
    		imageCache.put(imageFilename, maxSizeImage);
            		
            if (isCancelled()) return;
            setMaxSizeImage(maxSizeImage);
		}

		@Override
		protected void hadSuccess() {
            loadDone();
		}

		@Override
		protected void hadError(Throwable error) {
            loadError(error);
		}
    }


    public int getDisplaySize() {
        return displaySize;
    }

    public Icon getImage() {
        return imageLabel.getIcon();
    }

    public String getTitle() {
        return title;
    }

    public AnnotationTagCloudPanel getTagPanel() {
        return tagPanel;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public Entity getEntity() {
        return entity;
    }

    private synchronized void loadDone() {
        IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
        if (iconDemoPanel.isInverted()) {
            setInvertedColors(iconDemoPanel.isInverted());
        }
        else {
            rescaleImage(iconDemoPanel.getImagesPanel().getImageSize());
        }
        setImageLabel(imageLabel);
        iconDemoPanel.getImagesPanel().recalculateGrid();
    }
    
    private synchronized void loadError(Throwable error) {
        IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
        if (error instanceof FileNotFoundException) {
        	errorLabel.setText("File not found");
        }
        else {
        	error.printStackTrace();
            errorLabel.setText("Image could not be loaded");
        }
        setImageLabel(errorLabel);
        iconDemoPanel.getImagesPanel().recalculateGrid();
    }
    
	private synchronized void setMaxSizeImage(BufferedImage maxSizeImage) {
		if (viewable) this.maxSizeImage = maxSizeImage;
	}
	
    private synchronized void setImageLabel(JLabel label) {
        imagePanel.removeAll();
        imagePanel.add(label);
    }
}