package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;


/**
 * A DynamicImagePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an 
 * ImagesPanel.
 * 
 * TODO: this should be renamed "AnnotatedEntityButton" or something similar to indicate it is specific to displaying entities. 
 */
public class AnnotatedImageButton extends JToggleButton {

	private final JTextPane imageCaption;
    private final JPanel mainPanel;
    private final AnnotationTagCloudPanel tagPanel;

    // One of these goes in the mainPanel
    private BufferedImage staticIcon;
    private JComponent imageComponent;
    
    private Entity entity;
    
    public AnnotatedImageButton(final Entity entity) {
    	
        GridBagConstraints c = new GridBagConstraints();
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        add(buttonPanel);
    	
        imageCaption = new JTextPane();
        imageCaption.setFocusable(false);
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

        mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        buttonPanel.add(mainPanel, c);

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

				ModelMgr.getModelMgr().selectEntity(entity.getId(), false);
                
	            JPopupMenu popupMenu = new JPopupMenu();
	            
	            JMenuItem titleMenuItem = new JMenuItem(entity.getName());
	            titleMenuItem.setEnabled(false);
	            popupMenu.add(titleMenuItem);
	            
	            final String entityType = entity.getEntityType().getName();
	            if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) || entityType.equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
		            JMenuItem v3dMenuItem = new JMenuItem("  View in V3D (Neuron Annotator)");
		            v3dMenuItem.addActionListener(new ActionListener() {
		                public void actionPerformed(ActionEvent actionEvent) {
		    				try {
		    					Entity result = entity;
		    					if (!entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
			    					result = ModelMgr.getModelMgr().getAncestorWithType(entity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
		    					}
		    					
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
	            }
	            
//	            if (!entity.hasChildren()) {
//			        JMenuItem detailsMenuItem = new JMenuItem("  View details");
//		            detailsMenuItem.addActionListener(new ActionListener() {
//		                public void actionPerformed(ActionEvent actionEvent) {
//		    				// "View details" triggers an outline selection
//		    				ModelMgr.getModelMgr().selectEntity(entity.getId(), true);
//		                }
//		            });
//		            popupMenu.add(detailsMenuItem);
//	            }
	            
		        popupMenu.show(AnnotatedImageButton.this, e.getX(), e.getY());
			}

			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				// Double-clicking an image in gallery view triggers an outline selection
				ModelMgr.getModelMgr().selectEntity(entity.getId(), true);
			}
        	
        });
        
    	this.entity = entity;
    	
    	String title = entity.getName();
        if (title.length()>30) {
        	title = title.substring(0, 27) + "...";
        }
        
        String filepath = Utils.getDefaultImageFilePath(entity);
        if (filepath != null) {
	        File file = new File(PathTranslator.convertImagePath(filepath));
	        this.staticIcon = null;
	        this.imageComponent = new DynamicImagePanel(file.getAbsolutePath(), ImagesPanel.MAX_THUMBNAIL_SIZE);
        }
        else {
        	this.staticIcon = Icons.getLargeIconAsBufferedImage(entity);
        	this.imageComponent = new JLabel(new ImageIcon(staticIcon));
        }
        
        imageCaption.setText(title);
        mainPanel.add(imageComponent);
    }
    
	public synchronized void setTitleVisible(boolean visible) {
        imageCaption.setVisible(visible);
    }

    public synchronized void setTagsVisible(boolean visible) {
        tagPanel.setVisible(visible);
    }

    public AnnotationTagCloudPanel getTagPanel() {
        return tagPanel;
    }

    public Entity getEntity() {
        return entity;
    }

	public void cancelLoad() {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).cancelLoad();
		}
	}

	public void setCache(ImageCache imageCache) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).setCache(imageCache);
		}
	}

	public void rescaleImage(int imageSize) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).rescaleImage(imageSize);
	    	imageComponent.setPreferredSize(new Dimension(imageSize, imageSize));
		}
		else if (staticIcon!=null) {
			if (imageSize<staticIcon.getHeight() || imageSize<staticIcon.getWidth()) { // Don't scale up icons
				ImageIcon newIcon = new ImageIcon(Utils.getScaledImage(staticIcon, imageSize));
	        	((JLabel)imageComponent).setIcon(newIcon);
			}
	    	imageComponent.setPreferredSize(new Dimension(imageSize, imageSize));
	    	revalidate();
	    	repaint();
		}
	}

	public void setInvertedColors(boolean inverted) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).setInvertedColors(inverted);
		}
	}

	public void setViewable(boolean viewable) {
		if (imageComponent instanceof DynamicImagePanel) {
            final DynamicImagePanel dynamicImagePanel = ((DynamicImagePanel)imageComponent);
            dynamicImagePanel.setViewable(viewable, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					if (dynamicImagePanel.isViewable()) {
						// TODO: refactor this so it doesn't need to do this kind of dependency access
						IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
				        if (iconDemoPanel.isInverted()) {
				        	dynamicImagePanel.setInvertedColors(true);
				        }
				        else {
				        	dynamicImagePanel.rescaleImage(iconDemoPanel.getImagesPanel().getCurrImageSize());
				        }
					}
					return null;
				}
			});
		}
	}

}