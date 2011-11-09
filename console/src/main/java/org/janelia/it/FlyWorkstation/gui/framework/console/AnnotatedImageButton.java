package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;


/**
 * A DynamicImagePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an 
 * ImagesPanel.
 */
public abstract class AnnotatedImageButton extends JToggleButton {

	private final JLabel titleLabel;
    private final JPanel mainPanel;
    private final AnnotationTagCloudPanel tagPanel;

    // One of these goes in the mainPanel
    
    protected Entity entity;
    
    public AnnotatedImageButton(final Entity entity) {
    	
        GridBagConstraints c = new GridBagConstraints();
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        add(buttonPanel);

        titleLabel = new JLabel();
        titleLabel.setFocusable(false);
        titleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(false);
        
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(titleLabel, c);

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

        titleLabel.addMouseListener(new MouseAdapter() {
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
        
        titleLabel.setText(title);
        mainPanel.add(init(entity));
    }
    
    public abstract JComponent init(Entity entity);
    
	public synchronized void setTitleVisible(boolean visible) {
        titleLabel.setVisible(visible);
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

	public void rescaleImage(int imageSize) {
	}

	public void setInvertedColors(boolean inverted) {
	}

	public void setViewable(boolean viewable) {
	}

}