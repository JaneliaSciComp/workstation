package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityContextMenu;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
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
    private int annotationTableHeight = 100;
    
    protected Entity entity;
    
    public AnnotatedImageButton(final Entity entity) {

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
	            
				popupMenu.show(AnnotatedImageButton.this, e.getX(), e.getY());
			}

			@Override
			protected void doubleLeftClicked(MouseEvent e) {
				// Double-clicking an image in gallery view triggers an outline selection
            	String uniqueId = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline().getChildUniqueIdWithEntity(entity.getId());
            	if (Utils.isEmpty(uniqueId)) {
            		uniqueId = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline().getCurrUniqueId();
            	}
            	
            	if (Utils.isEmpty(uniqueId)) return;
            	
        		ModelMgr.getModelMgr().selectOutlineEntity(uniqueId, true);	
			}
        	
        });
        
        // Fix event dispatching so that user can click on the title or the tags and still select the button

        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	e.setSource(AnnotatedImageButton.this);
                AnnotatedImageButton.this.dispatchEvent(e);
            }
        });
    	
    	refresh(entity);
    }
    
    public void refresh(Entity entity) {

    	mainPanel.removeAll();
    	
    	String title = entity.getName();
        if (title.length()>30) {
        	title = title.substring(0, 27) + "...";
        }
        
        titleLabel.setText(title);
        titleLabel.setToolTipText(entity.getName());
        
        mainPanel.add(init(entity));
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
        JPanel annotationPanel = (JPanel)annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
        	annotationPanel.setPreferredSize(new Dimension(imageSize, annotationTableHeight));
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