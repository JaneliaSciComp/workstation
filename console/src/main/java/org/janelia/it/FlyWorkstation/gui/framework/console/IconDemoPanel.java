/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.dialogs.AnnotationDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationFilter;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * This panel shows images for annotation. It may show a bunch of images at once, or a single image.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel {

    private SplashPanel splashPanel;
    private JToolBar toolbar;
    private JToggleButton showTitlesButton;
    private JToggleButton showTagsButton;
    private JButton userButton;
    private JSlider slider;
    private JToggleButton invertButton;
    private JToggleButton onlySessionButton;
    private JToggleButton hideCompletedButton; 
    
    private ImagesPanel imagesPanel;
    private JScrollPane scrollPane;
    private ImageDetailPanel imageDetailPanel;
    private AnnotationDetailsDialog annotationDetailsDialog;

    private List<Entity> entities;
    private Entity currentEntity;
    private boolean viewingSingleImage = true;
    private double currImageSizePercent = 1.0;
    
    private final List<String> allUsers = new ArrayList<String>();
    private final Set<String> hiddenUsers = new HashSet<String>();
    private final Annotations annotations = new Annotations();

    private SimpleWorker entityLoadingWorker;
    private SimpleWorker annotationLoadingWorker;
    
    // Listen for key strokes and execute the appropriate key bindings
    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (KeymapUtil.isModifier(e)) return;

                KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
                if (SessionMgr.getKeyBindings().executeBinding(shortcut)) return;

                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    previousEntity();
                }
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    nextEntity();
                }
                
                revalidate();
                repaint();
            }
        }
    };

    // Listen for scroll events
    private final AdjustmentListener scrollListener = new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
    			@Override
    			public void run() {
    	        	loadUnloadImages();
    			}
    		});
        }
    };
    
    public IconDemoPanel() {

        setBackground(Color.white);
        setLayout(new BorderLayout());
        setFocusable(true);

        splashPanel = new SplashPanel();
        add(splashPanel);

        toolbar = createToolbar();
        imagesPanel = new ImagesPanel(this);
        imageDetailPanel = new ImageDetailPanel(this);
        annotationDetailsDialog = new AnnotationDetailsDialog();
        
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(imagesPanel);
        
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                double imageSizePercent = (double) source.getValue() / (double) 100;
                if (currImageSizePercent == imageSizePercent) return;
                currImageSizePercent = imageSizePercent;
                
                imagesPanel.rescaleImages(imageSizePercent);
	            imagesPanel.recalculateGrid();

	            // Only load images once we know how the layout will look 
	            // (i.e. after the above operations are completed and the panel is repainted)
                SwingUtilities.invokeLater(new Runnable() {
        			@Override
        			public void run() {
        	        	loadUnloadImages();
        			}
        		});
            }
        });
        
        this.addKeyListener(getKeyListener());
        
        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

			@Override
			public void annotationsChanged(long entityId) {
				AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
				if (button != null) {
					reloadAnnotations(button.getEntity());
				}
				filterEntities();
			}

			@Override
			public void sessionSelected(long sessionId) {
	        	loadImageEntities(ModelMgr.getModelMgr().getCurrentAnnotationSession().getEntities());
			}

			@Override
			public void sessionDeselected() {
	        	clear();
			}

			@Override
			public void entitySelected(long entityId) {
				AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
				if (button==null) return;
				setCurrentEntity(button.getEntity());
			}
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagesPanel.recalculateGrid();
            }
        });
        
    	annotations.setFilter(new AnnotationFilter() {
			@Override
			public boolean accept(OntologyAnnotation annotation) {
				// Hidden by user?
				if (hiddenUsers.contains(annotation.getOwner())) return false;
				AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
				// Hidden by session?
				if (!onlySessionButton.isSelected() || session == null) return true;
				// At this point we know there is a current session, and we have to match it
				return (annotation.getSessionId() != null && annotation.getSessionId().equals(session.getId()));
			}
		});
    	
    }

    private void setTitleVisbility() {
        for (AnnotatedImageButton button : getImagesPanel().getButtons().values()) {
            button.setTitleVisible(showTitlesButton.isSelected());
        }
    }

    private void setTagVisbility() {
        for (AnnotatedImageButton button : getImagesPanel().getButtons().values()) {
            button.setTagsVisible(showTagsButton.isSelected());
        }
    }
    
    private JToolBar createToolbar() {

        JToolBar toolBar = new JToolBar("Still draggable");
        toolBar.setFloatable(true);
        toolBar.setRollover(true);

        invertButton = new JToggleButton();
        invertButton.setIcon(Icons.getIcon("invert.png"));
        invertButton.setFocusable(false);
        invertButton.setToolTipText("Invert the color space on all images");
        invertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.setWaitingCursor(IconDemoPanel.this);
                try {
                    for (AnnotatedImageButton button : getImagesPanel().getButtons().values()) {
                        button.setInvertedColors(invertButton.isSelected());
                    }
                    getImagesPanel().repaint();
                }
                finally {
                    Utils.setDefaultCursor(IconDemoPanel.this);
                }
            }
        });
        toolBar.add(invertButton);
        
        showTitlesButton = new JToggleButton();
        showTitlesButton.setIcon(Icons.getIcon("text_smallcaps.png"));
        showTitlesButton.setFocusable(false);
        showTitlesButton.setSelected(true);
        showTitlesButton.setToolTipText("Show the image title above each image.");
        showTitlesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	setTitleVisbility();
                getImagesPanel().recalculateGrid();
            }
        });
        toolBar.add(showTitlesButton);

        showTagsButton = new JToggleButton();
        showTagsButton.setIcon(Icons.getIcon("page_white_stack.png"));
        showTagsButton.setFocusable(false);
        showTagsButton.setSelected(true);
        showTagsButton.setToolTipText("Show annotations below each image");
        showTagsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	setTagVisbility();
                getImagesPanel().recalculateGrid();
            }
        });
        toolBar.add(showTagsButton);

        onlySessionButton = new JToggleButton();
        onlySessionButton.setIcon(Icons.getIcon("cart.png"));
        onlySessionButton.setFocusable(false);
        onlySessionButton.setToolTipText("Only show annotations within the current annotation session");
        onlySessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	refreshAnnotations(null);
            }
        });
        toolBar.add(onlySessionButton);
        

        hideCompletedButton = new JToggleButton();
        hideCompletedButton.setIcon(Icons.getIcon("page_white_go.png"));
        hideCompletedButton.setFocusable(false);
        hideCompletedButton.setToolTipText("Hide images which have been annotated completely according to the annotation session's ruleset.");
        hideCompletedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
        		filterEntities();
            }
        });
        toolBar.add(hideCompletedButton);

        toolBar.addSeparator();

        userButton = new JButton("Annotations from...");
        userButton.setIcon(Icons.getIcon("group.png"));
        userButton.setFocusable(false);
        userButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	showPopupUserMenu();
            }
        });
        toolBar.add(userButton);
        
        toolBar.addSeparator();

        slider = new JSlider(10, 100, 100);
        slider.setFocusable(false);
        slider.setToolTipText("Image size percentage");
        toolBar.add(slider);

        return toolBar;
    }

    private void showPopupUserMenu() {

        final JPopupMenu userListMenu = new JPopupMenu();

        UserColorMapping userColors = ModelMgr.getModelMgr().getUserColorMapping();

        // Save the list of users so that when the function actually runs, the users it affects are the same
        // users that were displayed
        final List<String> savedUsers = new ArrayList<String>(allUsers);
        
        JMenuItem allUsersMenuItem = new JCheckBoxMenuItem("All users", hiddenUsers.isEmpty());
        allUsersMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (hiddenUsers.isEmpty()) {
                    for(String username : savedUsers) {
                    	hiddenUsers.add(username);
                    }
            	}
            	else {
            		hiddenUsers.clear();
            	}
            	refreshAnnotations(null);
            }
        });
        userListMenu.add(allUsersMenuItem);
        
        userListMenu.addSeparator();
        
        for(final String username : allUsers) {
            JMenuItem userMenuItem = new JCheckBoxMenuItem(username, !hiddenUsers.contains(username));
            userMenuItem.setBackground(userColors.getColor(username));
            userMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	if (hiddenUsers.contains(username))
                		hiddenUsers.remove(username);
                	else
                		hiddenUsers.add(username);
                	refreshAnnotations(null);
                }
            });
            userMenuItem.setIcon(Icons.getIcon("user.png"));
            userListMenu.add(userMenuItem);
        }
        
    	userListMenu.show(userButton, 0, userButton.getHeight());
    }
    
    public synchronized void loadImageEntities(final List<Entity> entities) {

    	// Remove the scroll listener so that we don't get a bunch of bogus events as things are added to the imagesPanel
    	scrollPane.getVerticalScrollBar().removeAdjustmentListener(scrollListener);

        if (entityLoadingWorker != null && !entityLoadingWorker.isDone()) {
        	System.out.println("Cancel previous image load");
        	entityLoadingWorker.disregard();
        }
        
    	entityLoadingWorker = new SimpleWorker() {

            protected void doStuff() throws Exception {
                List<Entity> loadedEntities = new ArrayList<Entity>();
                for (Entity entity : entities) {
                    if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D) 
                    		&& !entity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
                        // Ignore things we can't display
                        continue;
                    }
                    loadedEntities.add(entity);
                }
                setEntities(loadedEntities);
                annotations.init(loadedEntities);
            }

            protected void hadSuccess() {
            	entityLoadDone();
            }

            protected void hadError(Throwable error) {
            	entityLoadError(error);
            }
        };

        entityLoadingWorker.execute();
    }
    
    public synchronized void entityLoadDone() {

        setTitleVisbility();
        setTagVisbility();
        
        imagesPanel.setEntities(getEntities());
        refreshAnnotations(null);
        showAllEntities();
        filterEntities();
        
        // Since the images are not loaded yet, this will just resize the empty buttons so that we can calculate the grid correctly
        imagesPanel.rescaleImages(currImageSizePercent); 
        imagesPanel.recalculateGrid();
		
        
        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Reset scrollbar and re-add the listener
				scrollPane.getVerticalScrollBar().setValue(0); 
		        scrollPane.getVerticalScrollBar().addAdjustmentListener(scrollListener);
		        // Allow the images to load as needed, based on the viewport
				loadUnloadImages();
			}
		});
    }

    public synchronized void entityLoadError(Throwable error) {

        error.printStackTrace();
        if (getEntities() != null) {
            JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            imagesPanel.setEntities(getEntities());
            showAllEntities();
            // TODO: set read-only mode
        }
        else {
            JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading session", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
        }
    }

	private void filterEntities() {
		
		AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
		if (session == null) return;
		session.clearCompletedIds();
		Set<Long> completed = session.getCompletedEntityIds();
		
		for(AnnotatedImageButton button : imagesPanel.getButtons().values()) {
			if (hideCompletedButton.isSelected() && completed.contains(button.getEntity().getId())) {
				button.setVisible(false);
			}
			else {
				button.setVisible(true);
			}
		}
		
		imagesPanel.revalidate();
		imagesPanel.repaint();
	}
	
    /**
     * Reload the annotations from the database and then refresh the UI.
     */
    public synchronized void reloadAnnotations() {

        if (annotations == null || entities == null) return;

        annotationLoadingWorker = new SimpleWorker() {

            protected void doStuff() throws Exception {
                annotations.init(entities);
            }

            protected void hadSuccess() {
            	refreshAnnotations(null);
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        annotationLoadingWorker.execute();
    }

    /**
     * Reload the annotations from the database and then refresh the UI.
     */
    public synchronized void reloadAnnotations(final Entity entity) {

        if (annotations == null || entities == null) return;

        annotationLoadingWorker = new SimpleWorker() {

            protected void doStuff() throws Exception {
                annotations.init(entities);
            }

            protected void hadSuccess() {
            	refreshAnnotations(entity);
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        annotationLoadingWorker.execute();
    }

    /**
     * Refresh the annotation display in the UI, but do not reload anything from the database.
     */
    private synchronized void refreshAnnotations(Entity entity) {

    	// Refresh all user list
    	allUsers.clear();
    	for(OntologyAnnotation annotation : annotations.getAnnotations()) {
    		if (!allUsers.contains(annotation.getOwner())) allUsers.add(annotation.getOwner());
    	}
    	Collections.sort(allUsers);
    	
    	// Refresh the UI
        if (viewingSingleImage) imageDetailPanel.loadAnnotations(annotations);
        
    	if (entity == null) {
	        imagesPanel.loadAnnotations(annotations);
    	}
    	else {
	        imagesPanel.loadAnnotations(annotations, entity);
    	}
        
    }
    
    public synchronized void clear() {
    	this.entities = null;
    	this.currentEntity = null;
    	this.viewingSingleImage = false;
        removeAll();
        add(splashPanel, BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }

    public synchronized void showAllEntities() {
    	
        viewingSingleImage = false;
        removeAll();
        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        revalidate();
        repaint();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		        if (imagesPanel.getSelectedButton() != null) {
		        	imagesPanel.scrollRectToVisible(imagesPanel.getSelectedButton().getBounds());
		        }
			}
        });
        
        // Focus on the panel so that it can receive keyboard input
        requestFocusInWindow();
    }

    public synchronized void showCurrentEntityDetails() {
    	if (currentEntity == null) return;
        
    	imageDetailPanel.load(currentEntity);
    	if (annotations != null) {
        	imageDetailPanel.loadAnnotations(annotations);
        }
        
    	if (!viewingSingleImage) {
            viewingSingleImage = true;
            removeAll();
            add(imageDetailPanel);
        }

        revalidate();
        repaint();

        // Focus on the panel so that it can receive keyboard input
        requestFocusInWindow();
    }

    public synchronized boolean previousEntity() {
        List<Entity> entities = getEntities();
        int i = entities.indexOf(currentEntity);
        if (i < 1) {
            // Already at the beginning
            return false;
        }
        setCurrentEntity(entities.get(i - 1));
        if (viewingSingleImage) {
            showCurrentEntityDetails();
        }
        return true;
    }

    public synchronized boolean nextEntity() {
        List<Entity> entities = getEntities();
        int i = entities.indexOf(currentEntity);
        if (i > entities.size() - 2) {
            // Already at the end
            return false;
        }
        setCurrentEntity(entities.get(i + 1));
        if (viewingSingleImage) {
            showCurrentEntityDetails();
        }
        return true;
    }

    public synchronized List<Entity> getEntities() {
        return entities;
    }

    public synchronized void setEntities(List<Entity> entities) {
        this.entities = entities;
    }

    public synchronized Entity getCurrentEntity() {
        return currentEntity;
    }

    public synchronized void setCurrentEntity(Entity entity) {
        if (Utils.areSameEntity(entity, currentEntity)) return;
        this.currentEntity = entity;
        imagesPanel.setSelectedImage(currentEntity);
    	ModelMgr.getModelMgr().selectEntity(entity.getId());
    }

    public ImagesPanel getImagesPanel() {
        return imagesPanel;
    }

    public ImageDetailPanel getImageDetailPanel() {
        return imageDetailPanel;
    }

    public KeyListener getKeyListener() {
        return keyListener;
    }
    
    public boolean isInverted() {
        return invertButton.isSelected();
    }

	public void viewAnnotationDetails(OntologyAnnotation tag) {
		annotationDetailsDialog.showForAnnotation(tag);
	}

    // TODO: should this go in some kind of utility class?
    public String getFilePath(Entity entity) {
    	if (entity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
    		for(Entity childEntity : entity.getChildren()) {
    			if (childEntity.getEntityType().getName().equals(EntityConstants.TYPE_TIF_2D)) {
    	    		return childEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    			}
    		}
    		return null;
    	}
    	else {
    		return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	}
    }

    private void loadUnloadImages() {

        final JViewport viewPort = scrollPane.getViewport();
    	Rectangle viewRect = viewPort.getViewRect();
		
        for(AnnotatedImageButton button : imagesPanel.getButtons().values()) {
        	try {
        		button.setViewable(viewRect.intersects(button.getBounds()));
        	}
        	catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    }
}
