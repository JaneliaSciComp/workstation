package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A DynamicImagePanel with a title on top and optional annotation tags underneath. Made to be aggregated in an 
 * ImagesPanel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AnnotatedImageButton extends JToggleButton implements DragGestureListener { 
	
	private final JLabel titleLabel;
	private final JLabel subtitleLabel;
    private final JPanel mainPanel;
    private final JPanel buttonPanel;
    private final JLabel loadingLabel;
    private AnnotationView annotationView;
    private boolean annotationsLoaded = false;
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

        titleLabel = new JLabel(" ");
        titleLabel.setFocusable(false);
        titleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(false);

        subtitleLabel = new JLabel(" ");
        subtitleLabel.setFocusable(false);
        subtitleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subtitleLabel.setOpaque(false);

        loadingLabel = new JLabel();
        loadingLabel.setOpaque(false);
        loadingLabel.setIcon(Icons.getLoadingIcon());
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(titleLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 0;
        buttonPanel.add(subtitleLabel, c);
        
        mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 5, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        buttonPanel.add(mainPanel, c);
        
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add(loadingLabel, c);
        
        // Remove all default mouse listeners except drag gesture recognizer
        for(MouseListener mouseListener : getMouseListeners()) {
        	if (!(mouseListener instanceof MouseDragGestureRecognizer)) {
        		removeMouseListener(mouseListener);	
        	}
        }

        // Fix event dispatching so that user can click on the title or the tags and still select the button
        titleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        subtitleLabel.addMouseListener(new MouseForwarder(this, "JLabel(titleLabel)->AnnotatedImageButton"));
        
    	refresh(rootedEntity);
    }
    
    public void refresh(RootedEntity rootedEntity) {
    	
    	mainPanel.removeAll();
    	
        Entity entity = rootedEntity.getEntity();
        
        StringBuffer tsb = new StringBuffer();
        tsb.append(entity.getName());
        
        String splitPart = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
        if (splitPart!=null) {
        	tsb.append(" (").append(splitPart).append(")");
        }

        StringBuffer ssb = new StringBuffer();
        
        final Entity rep = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
        if (rep!=null) {
        	if (EntityUtils.isInitialized(rep)) {
        		ssb.append("Represented by ").append(rep.getName());	
        	}
        	else {
        		SimpleWorker worker = new SimpleWorker() {

        			private Entity loadedRep;
        			
					@Override
					protected void doStuff() throws Exception {
						loadedRep = ModelMgr.getModelMgr().getEntityById(rep.getId()+"");
					}
					
					@Override
					protected void hadSuccess() {
		        		String subtitle = "Represented by "+loadedRep.getName();
		        		setSubtitle(subtitle, 100);
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
					
				};
	        	worker.execute();
        	}
        }
        
    	setTitle(tsb.toString(), 100);
    	if (ssb.length()>0) {
    		setSubtitle(ssb.toString(), 100);
    	}
    	
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

    public void setSubtitle(String subtitle, int maxWidth) {
    	// Subtle font size scaling 
    	int fontSize = (int)Math.round((double)maxWidth*0.003)+10;
    	Font titleLabelFont = new Font("Sans Serif", Font.PLAIN, fontSize);
    	subtitleLabel.setFont(titleLabelFont);
    	subtitleLabel.setPreferredSize(new Dimension(maxWidth, subtitleLabel.getFontMetrics(titleLabelFont).getHeight()));
    	subtitleLabel.setText(subtitle);
    	subtitleLabel.setToolTipText(subtitle);
    }
    
    public abstract JComponent init(RootedEntity rootedEntity);
    
	public synchronized void setTitleVisible(boolean visible) {
        titleLabel.setVisible(visible);
        subtitleLabel.setVisible(visible);
    }

    public synchronized void setTagsVisible(boolean visible) {
    	if (annotationView==null) return;
        ((JPanel)annotationView).setVisible(visible);
    }

    public synchronized void setAnnotationView(AnnotationView annotationView) {
    	this.annotationView = annotationView;
        // Fix event dispatching so that user can click on the tags and still select the button
    	((JPanel)annotationView).addMouseListener(new MouseForwarder(this,"JPanel(annotationView)->AnnotatedImageButton"));
    	if (annotationsLoaded) {
    		showAnnotations(annotationView.getAnnotations());
    	}
    }
    
    public synchronized AnnotationView getAnnotationView() {
        return annotationView;
    }

    public synchronized void showAnnotations(List<OntologyAnnotation> annotations) {
    	
    	annotationView.setAnnotations(annotations);
    	annotationsLoaded = true;
    	
    	buttonPanel.remove(loadingLabel);
    	if (annotationView!=null) buttonPanel.remove((JPanel)annotationView);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weighty = 1;
        buttonPanel.add((JPanel)annotationView, c);
    }
    
    public RootedEntity getRootedEntity() {
        return rootedEntity;
    }
    
	public synchronized void rescaleImage(int width, int height) {
    	setTitle(titleLabel.getText(), width);
        JPanel annotationPanel = (JPanel)annotationView;
        if (annotationView instanceof AnnotationTablePanel) {
        	annotationPanel.setPreferredSize(new Dimension(width, annotationPanel.getPreferredSize().height));
        }
	}
	
	public synchronized void resizeTable(int tableHeight) {
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