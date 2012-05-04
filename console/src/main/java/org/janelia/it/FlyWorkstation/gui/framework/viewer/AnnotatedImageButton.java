package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityContextMenu;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * A DynamicImagePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an 
 * ImagesPanel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotatedImageButton extends JToggleButton implements DragGestureListener { 

	private final JLabel titleLabel;
    private final JPanel mainPanel;
    private final JPanel buttonPanel;
    private AnnotationView annotationView;
    private DragSource source;
    
    protected final IconDemoPanel iconDemoPanel;
    protected final Entity entity;
    
    public AnnotatedImageButton(final Entity entity, final IconDemoPanel iconDemoPanel) {

    	this.iconDemoPanel = iconDemoPanel;
    	this.entity = entity;
    	
    	this.source = new DragSource();
    	source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);
    			
		setTransferHandler(new EntityTransferHandler() {
			@Override
			public JComponent getDropTargetComponent() {
				return AnnotatedImageButton.this;
			}			
		});
				
        GridBagConstraints c = new GridBagConstraints();
        buttonPanel = new JPanel(new GridBagLayout());
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

        setAnnotationView(new AnnotationTagCloudPanel());
        
        // Remove all default mouse listeners except drag gesture recognizer
        for(MouseListener mouseListener : getMouseListeners()) {
        	if (!(mouseListener instanceof MouseDragGestureRecognizer)) {
        		removeMouseListener(mouseListener);	
        	}
        }

        // Mouse events
		
        this.addMouseListener(new MouseHandler() {

			@Override
			protected void popupTriggered(MouseEvent e) {
				if (e.isConsumed()) return;
				
				if (!isSelected()) {
					ModelMgr.getModelMgr().selectEntity(entity.getId(), true);
				}
				
				List<Long> entityIds = ModelMgr.getModelMgr().getSelectedEntitiesIds();
				
				JPopupMenu popupMenu = null;
				if (entityIds.size()>1) {
					popupMenu =  new JPopupMenu();

					String name = "(Multiple selected)";
			        JMenuItem titleMenuItem = new JMenuItem(name);
			        titleMenuItem.setEnabled(false);
			        popupMenu.add(titleMenuItem);
			        
			        // TODO: add a menu for doing things on multiple entities, such as removal
				}
				else {
					popupMenu = new EntityContextMenu(entity);
		            ((EntityContextMenu)popupMenu).addMenuItems();
				}
	            
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
				e.consume();
			}

			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				if (e.isConsumed()) return;
				
				// Double-clicking an image in gallery view triggers an outline selection
            	String uniqueId = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline().getChildUniqueIdWithEntity(entity.getId());
            	if (Utils.isEmpty(uniqueId)) {
            		uniqueId = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline().getCurrUniqueId();
            	}
            	
            	if (Utils.isEmpty(uniqueId)) return;
            	
        		ModelMgr.getModelMgr().selectOutlineEntity(uniqueId, true);	
        		e.consume();
			}

    		@Override
    		public void mouseReleased(MouseEvent e) {
    			if (e.isConsumed()) return;
    			super.mouseReleased(e);
    			
    			final AnnotatedImageButton button = AnnotatedImageButton.this;
    			final boolean shiftDown = e.isShiftDown();
    			final boolean metaDown = e.isMetaDown();
    			final boolean state = button.isSelected();
    			final Entity entity = button.getEntity();
    			final Long entityId = entity.getId();
    			
    			if (e.getClickCount() != 1) return;
    			
    			if (e.getButton() != MouseEvent.BUTTON1) {
    				if (!state) {
						ModelMgr.getModelMgr().selectEntity(entityId, true);
    				}
					return;
    			}
            	
    			SwingUtilities.invokeLater(new Runnable() {
    				@Override
    				public void run() {
    					// Now update the model
    					if (metaDown) {
    						// With the meta key we toggle items in the current
    						// selection without clearing it
    						if (!state) {
    							ModelMgr.getModelMgr().selectEntity(entityId, false);
    						} 
    						else {
    							ModelMgr.getModelMgr().deselectEntity(entityId);
    						}
    					} 
    					else {
    						// With shift, we select ranges
    						Long lastSelected = ModelMgr.getModelMgr().getLastSelectedEntityId();
    						if (shiftDown && lastSelected != null) {

    							// Walk through the buttons and select everything between the last and current selections
    							boolean selecting = false;
    							List<Entity> entities = iconDemoPanel.getEntities();
    							for (Entity entity : entities) {
    								if (entity.getId().equals(lastSelected) || entity.getId().equals(entityId)) {
    									if (entity.getId().equals(entityId)) {
    										// Always select the button that was clicked
    										ModelMgr.getModelMgr().selectEntity(entity.getId(), false);
    									}
    									if (selecting) return; // We already selected, this is the end
    									selecting = true; // Start selecting
    									continue; // Skip selection of the first and last items, which should already be selected
    								}
    								if (selecting) {
    									ModelMgr.getModelMgr().selectEntity(entity.getId(), false);
    								}
    							}
    						} 
    						else {
    							// This is a good old fashioned single button selection
    							ModelMgr.getModelMgr().selectEntity(entityId, true);
    						}

    					}

    					// Always request focus on the button that was clicked, 
    					// since other buttons may become selected if shift is involved
    					button.requestFocus();
    				}
    			});
    		}
    	});
        
        // Fix event dispatching so that user can click on the title or the tags and still select the button
        titleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        
    	refresh(entity);
    }
    
    public void refresh(Entity entity) {
    	mainPanel.removeAll();
    	setTitle(entity.getName(), 100);
        mainPanel.add(init(entity));
    }
    
    public void setTitle(String title, int maxWidth) {
    	// Subtle font size scaling 
    	int fontSize = (int)Math.round((double)maxWidth*0.005)+10;
    	Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
    	titleLabel.setFont(titleLabelFont);
    	titleLabel.setPreferredSize(new Dimension(maxWidth, titleLabel.getFontMetrics(titleLabelFont).getHeight()));
        titleLabel.setText(title);
        titleLabel.setToolTipText(title);
    }
    
    public abstract JComponent init(Entity entity);
    
	public synchronized void setTitleVisible(boolean visible) {
        titleLabel.setVisible(visible);
    }

    public synchronized void setTagsVisible(boolean visible) {
        ((JPanel)annotationView).setVisible(visible);
    }

    public void setAnnotationView(AnnotationView annotationView) {
    	
    	if (this.annotationView != null) {
    		buttonPanel.remove((JPanel)this.annotationView);
    	}
    	
    	this.annotationView = annotationView;

        // Fix event dispatching so that user can click on the tags and still select the button
    	((JPanel)annotationView).addMouseListener(new MouseForwarder(this,"JPanel(annotationView)->AnnotatedImageButton"));
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add((JPanel)annotationView, c);
    }
    
    public AnnotationView getAnnotationView() {
        return annotationView;
    }

    public Entity getEntity() {
        return entity;
    }

	public void rescaleImage(int imageSize) {
    	setTitle(entity.getName(), imageSize);
        JPanel annotationPanel = (JPanel)annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
        	annotationPanel.setPreferredSize(new Dimension(imageSize, annotationPanel.getPreferredSize().height));
        }
	}
	
	public void resizeTable(int tableHeight) {
        JPanel annotationPanel = (JPanel)annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
        	annotationPanel.setPreferredSize(new Dimension(annotationPanel.getPreferredSize().width, tableHeight));
        }
	}

	public void setInvertedColors(boolean inverted) {
	}

	public void setViewable(boolean viewable) {
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		
        if (!isSelected()) {
        	ModelMgr.getModelMgr().selectEntity(getEntity().getId(), true);
        }
		
		getTransferHandler().exportAsDrag(this, dge.getTriggerEvent(), TransferHandler.LINK);
	}
	
}