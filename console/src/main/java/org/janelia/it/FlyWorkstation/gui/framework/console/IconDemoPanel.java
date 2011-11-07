/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.Callable;

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
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutline;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
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
                if (!SessionMgr.getKeyBindings().executeBinding(shortcut)) {
	                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
	                    previousEntity();
	                }
	                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
	                    nextEntity();
	                }
                }
                
                revalidate();
                repaint();
            }
        }
    };

    private final FocusListener buttonFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
        	AnnotatedImageButton button = (AnnotatedImageButton)e.getSource();
			ModelMgr.getModelMgr().selectEntity(button.getEntity().getId(), false);

            // Scroll to the newly focused button
            imagesPanel.scrollRectToVisible(button.getBounds());
            imagesPanel.revalidate();
        }
    };
    
    public IconDemoPanel() {

        setBackground(Color.white);
        setLayout(new BorderLayout());
        setFocusable(true);

        splashPanel = new SplashPanel();
        add(splashPanel);

        toolbar = createToolbar();
        imagesPanel = new ImagesPanel();
        imagesPanel.setButtonKeyListener(keyListener);
        imagesPanel.setButtonFocusListener(buttonFocusListener);
        
        imageDetailPanel = new ImageDetailPanel(this);
        annotationDetailsDialog = new AnnotationDetailsDialog();
        
        
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                double imageSizePercent = (double) source.getValue() / (double) 100;
                if (currImageSizePercent == imageSizePercent) return;
                currImageSizePercent = imageSizePercent;
                imagesPanel.rescaleImages(imageSizePercent);
	            imagesPanel.recalculateGrid();
	            imagesPanel.loadUnloadImages();
            }
        });
        
        this.addKeyListener(getKeyListener());
        
        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

			@Override
			public void annotationsChanged(long entityId) {
				final AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
		        SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (button != null) reloadAnnotations(button.getEntity());
						filterEntities();
					}
				});
			}

			@Override
			public void sessionSelected(long sessionId) {
		        SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
			        	loadImageEntities(ModelMgr.getModelMgr().getCurrentAnnotationSession().getEntities());
					}
				});
			}

			@Override
			public void sessionDeselected() {
		        SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
			        	clear();
					}
				});
			}

			@Override
			public void entitySelected(final long entityId, final boolean outline) {
				
				// Find the entity object
				Entity selectedEntity = null;
				AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
				if (button != null) {
					selectedEntity = button.getEntity();
				}
				else {
					EntityOutline publicEntityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline();
					selectedEntity = publicEntityOutline.getEntityById(entityId);
					if (selectedEntity == null) {
						EntityOutline privateEntityOutline = SessionMgr.getSessionMgr().getActiveBrowser().getPrivateEntityOutline();
						selectedEntity = privateEntityOutline.getEntityById(entityId);
						if (selectedEntity==null) {
							System.out.println("Cannot find entity "+entityId+" in imagesPanel or either entity panel... falling back on DB.");
						}
					}
				}
				
				final Entity potentialEntity = selectedEntity;
				SimpleWorker worker = new SimpleWorker() {

					Entity entity = potentialEntity;
					
					@Override
					protected void doStuff() throws Exception {

		        		// In case we couldn't find the entity above 
						if (entity==null) {
		        			entity = ModelMgr.getModelMgr().getEntityById(entityId+"");
						}

		        		// In case we have a lazy entity, lets load the children
			        	if (!Utils.areLoaded(entity.getOrderedEntityData())) {
			                Utils.loadLazyEntity(entity, false);
			        	}
					}
					
					@Override
					protected void hadSuccess() {
						
						// In the EDT...
				        
				        if (outline) {
				        	List<Entity> entitiesToLoad = new ArrayList<Entity>();
				        	for(Entity child : entity.getOrderedChildren()) {
			        			entitiesToLoad.add(child);
				        	}
				        	if (entitiesToLoad.isEmpty()) {
				        		// No children, go straight to the leaf
				        		entitiesToLoad.add(entity);
				        		loadImageEntities(entitiesToLoad, new Callable<Void>() {
									@Override
									public Void call() throws Exception {
							        	setCurrentEntity(entity);
						        		showCurrentEntityDetails();
						        		imageDetailPanel.getIndexButton().setEnabled(false);
						        		return null;
									}
								});
				        	}
				        	else {
				        		// A bunch of children, show them all
				        		loadImageEntities(entitiesToLoad);
				        	}
				        }
				        else {
				        	setCurrentEntity(entity);
				        }
					}
					
					@Override
					protected void hadError(Throwable error) {
	                	SessionMgr.getSessionMgr().handleException(error);
					}
				};

				worker.execute();
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
                    imagesPanel.setInvertedColors(invertButton.isSelected());
                    imagesPanel.repaint();
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
            	imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
            	imagesPanel.recalculateGrid();
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
            	imagesPanel.setTagVisbility(showTagsButton.isSelected());
            	imagesPanel.recalculateGrid();
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

    private void loadImageEntities(final List<Entity> entities) {
    	loadImageEntities(entities, null);
    }

    private void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        this.updateUI();
    }
    
    private synchronized void loadImageEntities(final List<Entity> entities, final Callable<Void> success) {

    	showLoadingIndicator();
        
    	// Temporarily disable scroll loading 
		imagesPanel.setScrollLoadingEnabled(false);

        if (entityLoadingWorker != null && !entityLoadingWorker.isDone()) {
        	System.out.println("Cancel previous image load");
        	entityLoadingWorker.disregard();
        }
        
    	entityLoadingWorker = new SimpleWorker() {

            protected void doStuff() throws Exception {
                List<Entity> loadedEntities = new ArrayList<Entity>();
                for (Entity entity : entities) {
                    loadedEntities.add(entity);
                }
                setEntities(loadedEntities);
                annotations.init(loadedEntities);
            }

            protected void hadSuccess() {
            	entityLoadDone();
            	try {
            		if (success!=null) success.call();
            	}
            	catch (Exception e) {
            		SessionMgr.getSessionMgr().handleException(e);
            	}
            }

            protected void hadError(Throwable error) {
            	entityLoadError(error);
            }
        };

        entityLoadingWorker.execute();
    }
    
    private synchronized void entityLoadDone() {

    	if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");
    	
    	imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
    	imagesPanel.setTagVisbility(showTagsButton.isSelected());
        
        imagesPanel.setEntities(getEntities());
        refreshAnnotations(null);

        showAllEntities();
        filterEntities();
        
        // Since the images are not loaded yet, this will just resize the empty buttons so that we can calculate the grid correctly
        imagesPanel.rescaleImages(currImageSizePercent); 
        imagesPanel.recalculateGrid();
    	
		revalidate();
    	repaint();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				imagesPanel.setScrollLoadingEnabled(true);
		        imagesPanel.loadUnloadImages();
			}
		});
    }

    private synchronized void entityLoadError(Throwable error) {

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
		
//		imagesPanel.revalidate();
//		imagesPanel.repaint();
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
        add(imagesPanel, BorderLayout.CENTER);

        revalidate();
        repaint();

        // Wait until everything is recomputed
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		        if (getCurrentEntity() == null) return;
		        imagesPanel.scrollEntityToVisible(getCurrentEntity());
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
            imageDetailPanel.getIndexButton().setEnabled(true);
            imageDetailPanel.getPrevButton().setEnabled(entities.size()>1);
            imageDetailPanel.getNextButton().setEnabled(entities.size()>1);
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
        ModelMgr.getModelMgr().selectEntity(entities.get(i - 1).getId(), false);
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
		ModelMgr.getModelMgr().selectEntity(entities.get(i + 1).getId(), false);
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
        if (!imagesPanel.setSelectedImage(entity)) {
        	return;
        }
        this.currentEntity = entity;
    	ModelMgr.getModelMgr().selectEntity(entity.getId(), false);
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

	public double getCurrImageSizePercent() {
		return currImageSizePercent;
	}

	public void viewAnnotationDetails(OntologyAnnotation tag) {
		annotationDetailsDialog.showForAnnotation(tag);
	}
}
