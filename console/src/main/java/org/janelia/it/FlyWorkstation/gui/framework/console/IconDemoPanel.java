/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationToolbar;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This panel shows titled images in a grid with optional textual annotation tags beneath each one.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel {

    private static final String JACS_DATA_PATH_MAC = ConsoleProperties.getString("remote.defaultMacPath");
    private static final String JACS_DATA_PATH_LINUX = ConsoleProperties.getString("remote.defaultLinuxPath");
    
    private AnnotationSession session;
    
    private SplashPanel splashPanel;
    private AnnotationToolbar toolbar;
    private ImagesPanel imagesPanel;
    private JScrollPane scrollPane;
    private ImageDetailPanel imageDetailPanel;

    private List<Entity> entities;
    private Entity currentEntity;
	private boolean viewingSingleImage = true;
    
	public IconDemoPanel() {

        setBackground(Color.white);
        setLayout(new BorderLayout());

        splashPanel = new SplashPanel();
        add(splashPanel);
        
        toolbar = new AnnotationToolbar(this);
        imagesPanel = new ImagesPanel(this);
        imageDetailPanel = new ImageDetailPanel(this);
        
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagesPanel);
        
        toolbar.getSlider().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				double imageSizePercent = (double)source.getValue()/(double)100;
	    		imagesPanel.rescaleImages(imageSizePercent);
				imagesPanel.recalculateGrid();
			}
        });

        addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				imagesPanel.recalculateGrid();
			}
        	
        });

        imageDetailPanel.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int i = entities.indexOf(currentEntity);
				
				// Adjust current entity
				
				if (e.getWheelRotation() > 0) {
					if (i > entities.size()-2) {
						// Already at the end
						return;
					}
					setCurrentEntity(entities.get(i+1));
				}
				else {
					if (i < 1) {
						// Already at the beginning 
						return;
					}
					setCurrentEntity(entities.get(i-1));
				}
			
				showCurrentEntityDetails();
			}
		});
        
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
		        
				int value = e.getValue();
				// TODO: load/unload images as they move out of the viewport?
				
			}
		});
        
    }

	// TODO: need a more general way of doing this
    public String convertImagePath(String filepath) {
    	return filepath.replace(JACS_DATA_PATH_LINUX, JACS_DATA_PATH_MAC);
    }

    public void loadImageEntities(final AnnotationSession session) {
    
    	this.session = session;
    	
		SimpleWorker loadingWorker = new SimpleWorker() {

			private List<Entity> entities;
			private Map<Long, List<Entity>> annotationMap;

            protected void doStuff() throws Exception {
            	entities = session.getEntities();
                annotationMap = session.getAnnotationMap();
            }

			protected void hadSuccess() {
            	loadImageEntities(entities, annotationMap);
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				if (entities != null) {
					JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
					loadImageEntities(entities, new HashMap());
					// TODO: set read-only mode
				}
				else {
					JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading session", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
				}
            	
			}

        };

        loadingWorker.execute();
    }
    
    /**
     * Load the given image entities and display them in a grid. If files is null then redisplay the splash image.
     * @param entities List of entities
     * @param annotations
     */
    private void loadImageEntities(List<Entity> entities, Map<Long, List<Entity>> annotationMap) {
        
        try {
            this.entities = new ArrayList<Entity>();

			for (Entity entity : entities) {
				
				if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D)) {
					// Ignore things we can't display
					continue;
				}
				this.entities.add(entity);
			}
            imagesPanel.load(this.entities, annotationMap);
            
        	showAllEntities();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

	public void showCurrentEntityDetails() {
		imageDetailPanel.load(currentEntity, null);
		if (!viewingSingleImage) {
			viewingSingleImage  = true;
			removeAll();
			add(imageDetailPanel);
		}
        updateUI();
	}

	public void showAllEntities() {
		if (viewingSingleImage) {
			viewingSingleImage = false;
			removeAll();
	        add(toolbar, BorderLayout.NORTH);
	        add(scrollPane, BorderLayout.CENTER);
		}
        updateUI();
	}
	
    public ImagesPanel getImagesPanel() {
		return imagesPanel;
	}

    public Entity getCurrentEntity() {
		return currentEntity;
	}

	public void setCurrentEntity(Entity entity) {
    	this.currentEntity = entity;
    	imagesPanel.setSelectedImage(currentEntity);
    }

	public void refreshEntity(Entity entity) {

		session.clearDerivedProperties();
		
		if (viewingSingleImage) {
			if (currentEntity.getId().equals(entity.getId())) {
				// TODO: update tag cloud
			}
		}
		else {
			final AnnotatedImageButton button = imagesPanel.getButtons().get(entity.getId().toString());
			if (button != null) {
				SimpleWorker worker = new SimpleWorker() {

					@Override
					protected void doStuff() throws Exception {
						session.getAnnotationMap();
					}
					
					@Override
					protected void hadSuccess() {
						imagesPanel.updateTags(session.getAnnotationMap());
						Utils.setDefaultCursor(IconDemoPanel.this);
					}
					
					@Override
					protected void hadError(Throwable error) {
				    	Utils.setDefaultCursor(IconDemoPanel.this);
						error.printStackTrace();
					}
				};
				
				worker.execute();
			}
			
		}
		
	}
	
}
