package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.MouseListener;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;

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
    protected final RootedEntity rootedEntity;
    
    public AnnotatedImageButton(final RootedEntity rootedEntity, final IconDemoPanel iconDemoPanel) {

    	this.iconDemoPanel = iconDemoPanel;
    	this.rootedEntity = rootedEntity;
    	
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

        // Fix event dispatching so that user can click on the title or the tags and still select the button
        titleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        
    	refresh(rootedEntity);
    }
    
    public void refresh(RootedEntity rootedEntity) {
    	mainPanel.removeAll();
    	setTitle(rootedEntity.getEntity().getName(), 100);
        mainPanel.add(init(rootedEntity));
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
    
    public abstract JComponent init(RootedEntity rootedEntity);
    
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

    public RootedEntity getRootedEntity() {
        return rootedEntity;
    }
    
	public void rescaleImage(int width, int height) {
    	setTitle(rootedEntity.getEntity().getName(), width);
        JPanel annotationPanel = (JPanel)annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
        	annotationPanel.setPreferredSize(new Dimension(width, annotationPanel.getPreferredSize().height));
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
        	ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(iconDemoPanel.getSelectionCategory(), rootedEntity.getId(), true);
        }
		getTransferHandler().exportAsDrag(this, dge.getTriggerEvent(), TransferHandler.LINK);
	}

	public IconDemoPanel getIconDemoPanel() {
		return iconDemoPanel;
	}
}