package org.janelia.it.FlyWorkstation.gui.framework.console;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A lazy-loading image with a title on top and optional annotation tags underneath.
 */
public class AnnotatedImageButton extends JToggleButton {

    private JTextPane imageCaption;
    private final AnnotationTagCloudPanel tagPanel;
    private final JLabel imageLabel;
    private final String title;
    private final String imageFilename;
    private BufferedImage maxSizeImage;
    private BufferedImage invertedMaxSizeImage;
    private int displaySize;
    private boolean inverted = false;
    private Entity entity;

    public AnnotatedImageButton(String title, String imageFilename, final int index, final Entity entity) {
        this.entity = entity;
        this.title = title;
        this.imageFilename = imageFilename;

        GridBagConstraints c = new GridBagConstraints();
        final JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setOpaque(false);
        add(imagePanel);

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
        imagePanel.add(imageCaption, c);

        imageLabel = new JLabel();
        imageLabel.setSize(300, 300);
        imageLabel.setIcon(Icons.getLoadingIcon());
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.TOP);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        imagePanel.add(imageLabel, c);

        tagPanel = new AnnotationTagCloudPanel();

        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        imagePanel.add(tagPanel, c);


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

    public void setTitleVisible(boolean visible) {
        imageCaption.setVisible(visible);
    }

    public void setTagsVisible(boolean visible) {
        tagPanel.setVisible(visible);
    }

    public void loadImage(int imageSize) {

        try {
            this.displaySize = imageSize;
            maxSizeImage = Utils.getScaledImageIcon(Utils.readImage(imageFilename), imageSize);
            if (displaySize != imageSize) {
                rescaleImage(displaySize);
            }
            else {
                imageLabel.setIcon(new ImageIcon(maxSizeImage));
            }
        }
        catch (IOException e) {

            imageLabel.setForeground(Color.red);
            imageLabel.setIcon(Icons.getMissingIcon());
            imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
            imageLabel.setHorizontalTextPosition(JLabel.CENTER);

            if (e instanceof FileNotFoundException) {
                imageLabel.setText("File not found");
            }
            else {
                e.printStackTrace();
                imageLabel.setText("Image could not be loaded");
            }
        }
    }

    public void rescaleImage(int imageSize) {
        if (maxSizeImage == null) return;
        BufferedImage image = Utils.getScaledImageIcon(inverted ? invertedMaxSizeImage : maxSizeImage, imageSize);
        imageLabel.setIcon(new ImageIcon(image));
        this.displaySize = imageSize;
    }

    public void setInvertedColors(boolean inverted) {

        this.inverted = inverted;
        if (inverted == true) {
            invertedMaxSizeImage = Utils.invertImage(maxSizeImage);
        }
        else {
            // Free up memory when we don't need inverted images
            invertedMaxSizeImage = null;
        }

        rescaleImage(displaySize);
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

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}