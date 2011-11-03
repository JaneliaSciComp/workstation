package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

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

    private final Entity entity;
//    private final String title;
    private final BufferedImage staticIcon;
    
    private final JTextPane imageCaption;
    private final JComponent imageComponent;
    private final AnnotationTagCloudPanel tagPanel;
    
    public AnnotatedImageButton(String title, final int index, final Entity entity) {
    	
    	this.entity = entity;

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
        	this.staticIcon = Utils.toBufferedImage(Icons.getIcon(entity, true).getImage());
        	this.imageComponent = new JLabel(new ImageIcon(staticIcon));
        	((JLabel)imageComponent).setPreferredSize(new Dimension(ImagesPanel.MAX_THUMBNAIL_SIZE, ImagesPanel.MAX_THUMBNAIL_SIZE));
        }
        
        GridBagConstraints c = new GridBagConstraints();
        JPanel buttonPanel = new JPanel(new GridBagLayout());
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

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        buttonPanel.add(imageComponent, c);

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

		final IconDemoPanel iconDemoPanel = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel();
		
        this.addMouseListener(new MouseHandler() {

			@Override
			protected void popupTriggered(MouseEvent e) {

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
				// Double-clicking an image in gallery view triggers an outline selection
				ModelMgr.getModelMgr().selectEntity(entity.getId(), true);
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
		}
		else {
			if (staticIcon==null) throw new AssertionError("Image component is not a DynamicImagePanel but there is no static icon");
			if (imageSize<staticIcon.getHeight() || imageSize<staticIcon.getWidth()) { // Don't scale up icons
				ImageIcon newIcon = new ImageIcon(Utils.getScaledImage(staticIcon, imageSize));
	        	((JLabel)imageComponent).setIcon(newIcon);
			}
        	((JLabel)imageComponent).setPreferredSize(new Dimension(imageSize, imageSize));
        	imageComponent.invalidate();
		}
	}

	public void setInvertedColors(boolean inverted) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).setInvertedColors(inverted);
		}
	}

	public void setViewable(boolean viewable) {
		if (imageComponent instanceof DynamicImagePanel) {
			((DynamicImagePanel)imageComponent).setViewable(viewable);
		}
	}

}