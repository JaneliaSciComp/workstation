/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationToolbar;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This panel shows titled images in a grid with optional textual annotation tags beneath each one.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel {

    private SplashPanel splashPanel;
    private AnnotationToolbar toolbar;
    private ImagesPanel imagesPanel;
    private JScrollPane scrollPane;

    public IconDemoPanel() {

        setBackground(Color.white);
        setLayout(new BorderLayout(0,0));

        splashPanel = new SplashPanel();
        toolbar = new AnnotationToolbar();
        imagesPanel = new ImagesPanel();
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagesPanel);
        
        toolbar.getSlider().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
	    		imagesPanel.rescaleImages((double)source.getValue()/(double)100);
				imagesPanel.recalculateGrid();
			}
        });

        addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				imagesPanel.recalculateGrid();
			}
        	
        });
    }

//    /**
//     * Load the given image files and display them in a grid. If files is null then redisplay the splash image.
//     * @param files
//     */
//    public void loadImages(List<File> files) {
//
//        try {
//            remove(splashPanel);
//
//            if (files == null) {
//                add(splashPanel);
//                return;
//            }
//
//            add(toolbar, BorderLayout.NORTH);
//            add(scrollPane, BorderLayout.CENTER);
//
//            List<String> labels = new ArrayList<String>();
//            List<String> filenames = new ArrayList<String>();
//
//			for (File file : files) {
//				labels.add(file.getName());
//				filenames.add(file.getAbsolutePath());
//			}
//
//            imagesPanel.load(labels, filenames);
//            SwingUtilities.updateComponentTreeUI(IconDemoPanel.this);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void loadImageEntities(final List<Entity> entities) {
    
		SimpleWorker loadingWorker = new SimpleWorker() {

			private List<Entity> annotations;

            protected void doStuff() throws Exception {
            	annotations = EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntities(
    	                (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME), entities);
            }

			protected void hadSuccess() {
            	loadImageEntities(entities, annotations);
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            	loadImageEntities(entities, new ArrayList<Entity>());
            	// TODO: set read-only mode
			}

        };

        loadingWorker.execute();
    }
    
    public void loadImageEntities(final AnnotationSession session) {
    
		SimpleWorker loadingWorker = new SimpleWorker() {

			private List<Entity> entities;
			private List<Entity> annotations;

            protected void doStuff() throws Exception {
            	entities = session.getEntities();
                annotations = session.getAnnotations();
                
                // TODO: remove debugging code
//                for(Entity entity : session.getAnnotationMap().keySet()) {
//                	List<Entity> annots = session.getAnnotationMap().get(entity);
//                	
//                	System.out.println(entity.getName()+" : ");
//                	for(Entity annot : annots) {
//                		System.out.println("  "+annot.getName()+" ");	
//                		System.out.println("      "+annot.getUser().getUserLogin()+" ");	
//                		System.out.println("      "+annot.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM)+" ");	
//                		System.out.println("      "+annot.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM)+" ");	
//                		System.out.println("      "+annot.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID)+" ");	
//                	}
//                }
            }

			protected void hadSuccess() {
            	loadImageEntities(entities, annotations);
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				if (entities != null) {
					JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
					loadImageEntities(entities, new ArrayList<Entity>());
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
    public void loadImageEntities(List<Entity> entities, List<Entity> annotations) {
    	
        try {
            remove(splashPanel);
           
            if (entities == null) {
                add(splashPanel);
                return;
            }

            add(toolbar, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            List<Entity> renderedEntities = new ArrayList<Entity>();

			for (Entity entity : entities) {
				
				if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D)) {
					// Ignore things we can't display
					continue;
				}
				renderedEntities.add(entity);
			}

            imagesPanel.load(renderedEntities, annotations);
            revalidate();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Return the currently selected image.
     * @return
     */
    public AnnotatedImageButton getSelectedImage() {
    	// TODO: this should probably return an Entity, not the Component
    	return imagesPanel.getSelectedImage();
    }

    /**
     * Add or remove the given tag from the currently selected image.
     * @param tag
     */
    public boolean addOrRemoveTag(String tag) {
        return imagesPanel.addOrRemoveTag(tag);
    }

}
