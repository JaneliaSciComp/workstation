/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.actions.Action;
import org.janelia.it.FlyWorkstation.gui.framework.actions.RemoveEntityAction;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.util.*;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This viewer shows images in a grid. It is modeled after OS X Finder. It wraps an ImagesPanel and provides a lot of 
 * functionality on top of it, such as:
 * 1) Asynchronous entity loading
 * 2) Entity selection and navigation
 * 3) Toolbar with various features 
 * 4) HUD display for currently selected image
 * 5) Pagination
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends Viewer {

	protected final static int PAGE_SIZE = 500;
	
	// Main components
	protected SplashPanel splashPanel;
	protected IconDemoToolbar iconDemoToolbar;
	protected ImagesPanel imagesPanel;
	
	// Status bar
	protected JPanel statusBar;
	protected JLabel statusLabel;
	protected JButton prevPageButton;
	protected JButton nextPageButton;
	protected JLabel pagingStatusLabel;
	
	// Hud dialog
	protected Hud hud;
	
	// These members deal with the context and entities within it
	protected RootedEntity contextRootedEntity;
	protected List<RootedEntity> allRootedEntities;
	protected List<RootedEntity> rootedAncestors;
	protected int numPages;
	
	// These members deal with entities on the current page only
	protected int currPage;
	protected List<RootedEntity> pageRootedEntities;
	protected Map<String,RootedEntity> pageRootedEntityMap;
	protected Map<Long,Entity> entityMap;
	protected final Annotations annotations = new Annotations();
	protected final List<String> allUsers = new ArrayList<String>();
	protected final Set<String> hiddenUsers = new HashSet<String>();
	protected int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
	protected final List<String> allImageRoles = new ArrayList<String>();
	protected String currImageRole;

	// Tracking of loading operations
	protected AtomicBoolean entityLoadInProgress = new AtomicBoolean(false);
	protected AtomicBoolean annotationLoadInProgress = new AtomicBoolean(false);
	protected AtomicBoolean ancestorLoadInProgress = new AtomicBoolean(false);
	protected SimpleWorker entityLoadingWorker;
	protected SimpleWorker annotationLoadingWorker;
	protected SimpleWorker annotationsInitWorker;
	protected SimpleWorker ancestorLoadingWorker;
	
	// Listen for key strokes and execute the appropriate key bindings
	// TODO: we should replace this with an action map in the future
	protected KeyListener keyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {

			if (KeymapUtil.isModifier(e)) return;
			if (e.getID() != KeyEvent.KEY_PRESSED) return;

			KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
			if (!SessionMgr.getKeyBindings().executeBinding(shortcut)) {

				// No keybinds matched, use the default behavior

				// Ctrl-A or Meta-A to select all
				if (e.getKeyCode() == KeyEvent.VK_A && ((SystemInfo.isMac && e.isMetaDown()) || (e.isControlDown()))) {
					for (RootedEntity rootedEntity : pageRootedEntities) { 
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), false);
					}
					return;
				}
				
				// Space on a single entity triggers a preview 
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					updateHud();
					hud.toggleDialog();
					e.consume();
					return;
				}

				// Enter with a single entity selected triggers an outline
				// navigation
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
					if (selectedIds.size() != 1) return;
					String selectedId = selectedIds.get(0);
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, selectedId, true);
					return;
				}
				
				// Delete triggers deletion
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					List<RootedEntity> selected = getSelectedEntities();
					if (selected.isEmpty()) return;
					final Action action = new RemoveEntityAction(selected);
					action.doAction();
					e.consume();
					return;
				}

				// Tab and arrow navigation to page through the images
				boolean clearAll = false;
				RootedEntity rootedEntity = null;
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					clearAll = true;
					if (e.isShiftDown()) {
						rootedEntity = getPreviousEntity();
					} else {
						rootedEntity = getNextEntity();
					}
				} else {
					clearAll = true;
					if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						rootedEntity = getPreviousEntity();
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						rootedEntity = getNextEntity();
					}
				}

				if (rootedEntity != null) {
					AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
					if (button != null) {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), clearAll);
						imagesPanel.scrollEntityToCenter(rootedEntity);
						button.requestFocus();
					}
				}
			}

			revalidate();
			repaint();
		}
	};

	// Listener for clicking on buttons
	protected MouseListener buttonMouseListener = new MouseHandler() {

		@Override
		protected void popupTriggered(MouseEvent e) {
			if (e.isConsumed()) return;
			AnnotatedImageButton button = getButtonAncestor(e.getComponent());
			// Select the button first
			RootedEntity rootedEntity = button.getRootedEntity();
			if (!button.isSelected()) {
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), true);
			}
			getButtonPopupMenu().show(e.getComponent(), e.getX(), e.getY());
			e.consume();
		}

		@Override
		protected void doubleLeftClicked(MouseEvent e) {
			if (e.isConsumed()) return;
			AnnotatedImageButton button = getButtonAncestor(e.getComponent());
			buttonDrillDown(button);
			// Double-clicking an image in gallery view triggers an outline selection
    		e.consume();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
			if (e.isConsumed()) return;
			AnnotatedImageButton button = getButtonAncestor(e.getComponent());			
			if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 0) {
				return;
			}
			buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
		}
	};
	
	protected JPopupMenu getButtonPopupMenu() {
		List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
		List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
		for (String entityId : selectionIds) {
			rootedEntityList.add(getRootedEntityById(entityId));
		}
		JPopupMenu popupMenu = new EntityContextMenu(rootedEntityList);
		((EntityContextMenu)popupMenu).addMenuItems();
		return popupMenu;
	}
	
	/**
	 * This is a separate method so that it can be overridden to accomodate other behavior patterns.
	 * @param button
	 */
	protected void buttonDrillDown(AnnotatedImageButton button) {
		RootedEntity rootedEntity = button.getRootedEntity();
		RootedEntity contextRootedEntity = getContextRootedEntity();
		if (contextRootedEntity==null || contextRootedEntity==rootedEntity) return;
    	if (StringUtils.isEmpty(rootedEntity.getUniqueId())) return;
		ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);	
	}
	
	protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
		final String category = getSelectionCategory();
		final RootedEntity rootedEntity = button.getRootedEntity();
		final String rootedEntityId = rootedEntity.getId();
		
		if (multiSelect) {
			// With the meta key we toggle items in the current
			// selection without clearing it
			if (!button.isSelected()) {
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, rootedEntityId, false);
			} 
			else {
				ModelMgr.getModelMgr().getEntitySelectionModel().deselectEntity(category, rootedEntityId);
			}
		} 
		else {
			// With shift, we select ranges
			String lastSelected = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(getSelectionCategory());
			if (rangeSelect && lastSelected != null) {
				// Walk through the buttons and select everything between the last and current selections
				boolean selecting = false;
				List<RootedEntity> rootedEntities = getRootedEntities();
				for (RootedEntity otherRootedEntity : rootedEntities) {
					if (otherRootedEntity.getId().equals(lastSelected) || otherRootedEntity.getId().equals(rootedEntityId)) {
						if (otherRootedEntity.getId().equals(rootedEntityId)) {
							// Always select the button that was clicked
							ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
						}
						if (selecting) return; // We already selected, this is the end
						selecting = true; // Start selecting
						continue; // Skip selection of the first and last items, which should already be selected
					}
					if (selecting) {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
					}
				}
			} 
			else {
				// This is a good old fashioned single button selection
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, rootedEntityId, true);
			}
		}
		
		button.requestFocus();
	}
	
	private AnnotatedImageButton getButtonAncestor(Component component) {
		Component c = component;
		while (!(c instanceof AnnotatedImageButton)) {
			c = c.getParent();
		}
		return (AnnotatedImageButton)c;
	}
	
	public IconDemoPanel(final String selectionCategory) {
		this(null, selectionCategory);
	}
	
	public IconDemoPanel(final ViewerSplitPanel viewerContainer, final String selectionCategory) {

		super(viewerContainer, selectionCategory);
		
		currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;
		
		setBorder(BorderFactory.createEmptyBorder());
		setLayout(new BorderLayout());
		setFocusable(true);

        SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {
            @Override
            public void browserAdded(BrowserModel browserModel) {
            }

            @Override
            public void browserRemoved(BrowserModel browserModel) {
            }

            @Override
            public void sessionWillExit() {
            }

            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if(key == "console.serverLogin"){
                    IconDemoPanel.this.clear();
                }
            }
        });

		hud = new Hud();
		hud.addKeyListener(keyListener);
		
		splashPanel = new SplashPanel();
		add(splashPanel);

		iconDemoToolbar = createToolbar();
		
		imagesPanel = new ImagesPanel(this);
		imagesPanel.setButtonKeyListener(keyListener);
		imagesPanel.setButtonMouseListener(buttonMouseListener);
		imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconDemoPanel"));
		iconDemoToolbar.addMouseListener(new MouseForwarder(this, "JToolBar->IconDemoPanel"));
		
		prevPageButton = new JButton(Icons.getIcon("arrow_back.gif"));
		prevPageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goPrevPage();
			}
		});
		
		nextPageButton = new JButton(Icons.getIcon("arrow_forward.gif"));
		nextPageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goNextPage();
			}
		});

        statusLabel = new JLabel("");
		pagingStatusLabel = new JLabel("");
		
		statusBar = new JPanel();
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
		statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color)UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
		
		statusBar.add(Box.createRigidArea(new Dimension(10,20)));
        statusBar.add(statusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10,20)));
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(pagingStatusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10,20)));
		statusBar.add(prevPageButton);
		statusBar.add(nextPageButton);
		
		addKeyListener(keyListener);

		imagesPanel.addMouseListener(new MouseHandler() {
			@Override
			protected void popupTriggered(MouseEvent e) {
				if (contextRootedEntity==null) return;
				JPopupMenu popupMenu = new JPopupMenu();
				JMenuItem titleItem = new JMenuItem("" +contextRootedEntity.getEntity().getName());
				titleItem.setEnabled(false);
				popupMenu.add(titleItem);

				JMenuItem newFolderItem = new JMenuItem("  Create new folder");
				newFolderItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent actionEvent) {

						// Add button clicked
						String folderName = (String) JOptionPane.showInputDialog(IconDemoPanel.this, "Folder Name:\n",
								"Create folder under " + contextRootedEntity.getEntity().getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
						if ((folderName == null) || (folderName.length() <= 0)) {
							return;
						}

						try {
							// Update database
							Entity parentFolder = contextRootedEntity.getEntity();
							Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
							ModelMgr.getModelMgr().addEntityToParent(parentFolder, newFolder,
									parentFolder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);

						} 
						catch (Exception ex) {
							SessionMgr.getSessionMgr().handleException(ex);
						}
					}
				});

				if (!contextRootedEntity.getEntity().getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)
						|| !contextRootedEntity.getEntity().getUser().getUserLogin().equals(SessionMgr.getUsername())) {
					newFolderItem.setEnabled(false);
				}
				
				popupMenu.add(newFolderItem);
				popupMenu.show(imagesPanel, e.getX(), e.getY());	
			}
		});
		
		ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

			@Override
			public void annotationsChanged(final long entityId) {
				if (pageRootedEntities!=null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							reloadAnnotations(entityId);
							filterEntities();
						}
					});	
				}
			}
			
			@Override
			public void entitySelected(String category, String entityId, boolean clearAll) {
				if (category.equals(selectionCategory)) {
					IconDemoPanel.this.entitySelected(entityId, clearAll);
				}
			}

			@Override
			public void entityDeselected(String category, String entityId) {
				if (category.equals(selectionCategory)) {
					IconDemoPanel.this.entityDeselected(entityId);
				}
			}

			@Override
			public void entityChanged(final long entityId) {
				if (contextRootedEntity==null) return;
				if (contextRootedEntity.getEntity().getId().equals(entityId)) {
					refresh();	
				}
				else {
					SimpleWorker worker = new SimpleWorker() {
						private Entity newEntity;
						
						@Override
						protected void doStuff() throws Exception {
							newEntity = ModelMgr.getModelMgr().getEntityById(entityId+"");
						}
						
						@Override
						protected void hadSuccess() {
							for(AnnotatedImageButton button : imagesPanel.getButtonsByEntityId(entityId)) {
								RootedEntity rootedEntity = button.getRootedEntity();
								if (rootedEntity != null) {
									ModelMgrUtils.updateEntity(rootedEntity.getEntity(), newEntity);	
									button.refresh(rootedEntity);
									button.setViewable(true);
								}
							}
						}
						
						@Override
						protected void hadError(Throwable error) {
							SessionMgr.getSessionMgr().handleException(error);
						}
					};
					
					worker.execute();
				}
			}
			
			@Override
			public void entityRemoved(long entityId) {
				if (contextRootedEntity==null) return;
				if (contextRootedEntity.getEntity()!=null && contextRootedEntity.getEntityId().equals(entityId)) {
					goParent();
				}
				else {
					for(RootedEntity rootedEntity : new ArrayList<RootedEntity>(pageRootedEntities)) {
						if (rootedEntity.getEntityId().equals(entityId)) {
							removeRootedEntity(rootedEntity);
							return;
						}
					}	
				}
			}

			@Override
			public void entityDataRemoved(long entityDataId) {
				if (contextRootedEntity==null) return;
				if (contextRootedEntity.getEntityData().getId()!=null && contextRootedEntity.getEntityData().getId().equals(entityDataId)) {
					goParent();
				}
				else {
					for(RootedEntity rootedEntity : new ArrayList<RootedEntity>(pageRootedEntities)) {
						EntityData entityData = rootedEntity.getEntityData();
						if (entityData.getId()!=null && entityData.getId().equals(entityDataId)) {
							removeRootedEntity(rootedEntity);
							return;
						}
					}
				}
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
				Boolean onlySession = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
						ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY);
				if ((onlySession!=null && !onlySession) || session == null) return true;
				
				// At this point we know there is a current session, and we have to match it
				return (annotation.getSessionId() != null && annotation.getSessionId().equals(session.getId()));
			}
		});

		SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {
			
			@Override
			public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
				
				if (ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY.equals(key)) {
					Utils.setWaitingCursor(IconDemoPanel.this);
					try {
						imagesPanel.setInvertedColors((Boolean)newValue);
						imagesPanel.repaint();
					} finally {
						Utils.setDefaultCursor(IconDemoPanel.this);
					}	
				}
				else if (ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY.equals(key)) {
					refreshAnnotations(null);
				}
				else if (ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY.equals(key)) {
					filterEntities();
				}
				else if (ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY.equals(key)) {
					refresh();
				}
				else if (ViewerSettingsPanel.ANNOTATION_TABLES_HEIGHT_PROPERTY.equals(key)) {
					int tableHeight = (Integer)newValue;
					if (currTableHeight == tableHeight) return;
					currTableHeight = tableHeight;
					imagesPanel.resizeTables(tableHeight);
					imagesPanel.rescaleImages(iconDemoToolbar.getCurrImageSize());
					imagesPanel.recalculateGrid();
					imagesPanel.scrollSelectedEntitiesToCenter();
					imagesPanel.loadUnloadImages();
				}
			}
			
			@Override
			public void sessionWillExit() {
			}
			
			@Override
			public void browserRemoved(BrowserModel browserModel) {
			}
			
			@Override
			public void browserAdded(BrowserModel browserModel) {
			}
		});
	}
	
	protected IconDemoToolbar createToolbar() {

		return new IconDemoToolbar() {

			protected void goBack() {
				final EntitySelectionHistory history = getEntitySelectionHistory();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						history.goBack();
					}
				});
			}

			protected void goForward() {
				final EntitySelectionHistory history = getEntitySelectionHistory();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						history.goForward();
					}
				});
			}

			protected void refresh() {
				IconDemoPanel.this.refresh();
			}

			protected void showTitlesButtonPressed() {
				imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
				imagesPanel.recalculateGrid();
			}
			
			protected void showTagsButtonPressed() {
				imagesPanel.setTagVisbility(showTagsButton.isSelected());
				imagesPanel.recalculateGrid();
			}
			
			protected void currImageSizeChanged(int imageSize) {
				imagesPanel.rescaleImages(imageSize);
				imagesPanel.recalculateGrid();
			}
			
			protected JPopupMenu getPopupPathMenu() {
				if (rootedAncestors==null) return null;
				final JPopupMenu pathMenu = new JPopupMenu();
				for (final RootedEntity ancestor : rootedAncestors) {
					JMenuItem pathMenuItem = new JMenuItem(ancestor.getEntity().getName(), Icons.getIcon(ancestor.getEntity()));
					pathMenuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							goEntity(ancestor.getUniqueId());
						}
					});
					pathMenuItem.setEnabled(pathMenu.getComponentCount()>0);
					pathMenu.add(pathMenuItem);
				}
				return pathMenu;
			}
			
			protected JPopupMenu getPopupUserMenu() {
				final JPopupMenu userListMenu = new JPopupMenu();
				UserColorMapping userColors = ModelMgr.getModelMgr().getUserColorMapping();

				// Save the list of users so that when the function actually runs, the
				// users it affects are the same users that were displayed
				final List<String> savedUsers = new ArrayList<String>(allUsers);

				JMenuItem allUsersMenuItem = new JCheckBoxMenuItem("All users", hiddenUsers.isEmpty());
				allUsersMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (hiddenUsers.isEmpty()) {
							for (String username : savedUsers) {
								hiddenUsers.add(username);
							}
						} else {
							hiddenUsers.clear();
						}
						refreshAnnotations(null);
					}
				});
				userListMenu.add(allUsersMenuItem);

				userListMenu.addSeparator();

				for (final String username : savedUsers) {
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
				
				return userListMenu;
			}
			
			protected JPopupMenu getPopupImageRoleMenu() {

				final JPopupMenu imageRoleListMenu = new JPopupMenu();
				final List<String> imageRoles = new ArrayList<String>(allImageRoles);

				for (final String imageRole : imageRoles) {
					JMenuItem roleMenuItem = new JCheckBoxMenuItem(imageRole, imageRole.equals(currImageRole));
					roleMenuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							currImageRole = imageRole;
							entityLoadDone(null);
						}
					});
					imageRoleListMenu.add(roleMenuItem);
				}
				return imageRoleListMenu;
			}
		};
	}

	protected void entitySelected(String entityId, boolean clearAll) {
		imagesPanel.setSelection(entityId, true, clearAll);
		updateHud();
		updateStatusBar();
	}

	public void entityDeselected(String entityId) {
		imagesPanel.setSelection(entityId, false, false);
		updateHud();
		updateStatusBar();
	}
	
	private void updateStatusBar() {
		if (pageRootedEntities==null) return;
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		int s = esm.getSelectedEntitiesIds(getSelectionCategory()).size();
		statusLabel.setText(s+" of "+allRootedEntities.size()+" selected");
	}

	
	private void updateHud() {
		List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
		if (selectedIds.size() != 1) {
			hud.hideDialog();
			return;
		}
		String selectedId = selectedIds.get(0);
		for(RootedEntity re : getRootedEntitiesById(selectedId)) {
			final AnnotatedImageButton button = imagesPanel.getButtonById(re.getId());
			if (button instanceof DynamicImageButton) {
				final DynamicImageButton d = (DynamicImageButton)button;
				hud.setEntityId(re.getEntity().getId());
				BufferedImage bufferedImage = d.getDynamicImagePanel().getMaxSizeImage();
				if (bufferedImage==null) {
					return;
				}
				hud.setTitle(button.getRootedEntity().getEntity().getName());
				hud.setImage(bufferedImage);
				hud.pack();
				return; // There can be only one!
			}
		}
	}
	

	public synchronized void goParent() {
		final String selectedUniqueId = contextRootedEntity.getUniqueId();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String parentId = Utils.getParentIdFromUniqueId(selectedUniqueId);
				if (StringUtils.isEmpty(parentId)) {
					clear();
				}
				else {
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, parentId, true);	
				}
			}
		});
	}

	public synchronized void goEntity(final String uniqueId) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId, true);	
			}
		});
	}

	private void updatePagingStatus() {
		prevPageButton.setEnabled(currPage>0);
		nextPageButton.setEnabled(currPage<numPages-1);
	}
	
	private synchronized void goPrevPage() {
		int page = currPage-1;
		if (page < 0) return;
		loadImageEntities(page, null);
	}
	
	private synchronized void goNextPage() {
		int page = currPage+1;
		if (page >= numPages) return;
		loadImageEntities(page, null);
	}

	public void showLoadingIndicator() {
		removeAll();
		add(new JLabel(Icons.getLoadingIcon()));
		this.updateUI();
	}
	
	@Override
	public void loadEntity(RootedEntity rootedEntity) {
		loadEntity(rootedEntity, null);
	}
	
	public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {
		
		this.contextRootedEntity = rootedEntity;
		if (contextRootedEntity==null) return;
		
		Entity entity = contextRootedEntity.getEntity();

		getEntitySelectionHistory().pushHistory(contextRootedEntity.getUniqueId());
		setTitle(entity.getName());
		
		List<EntityData> eds = entity.getOrderedEntityData();
		List<EntityData> children = new ArrayList<EntityData>();
		for(EntityData ed : eds) {
			Entity child = ed.getChildEntity();
			if (!EntityUtils.isHidden(ed) && child!=null) {
				children.add(ed);
			}
		}
		
		List<RootedEntity> lazyRootedEntities = new ArrayList<RootedEntity>();
		for(EntityData ed : children) {
			String childId = EntityOutline.getChildUniqueId(rootedEntity.getUniqueId(), ed);
			lazyRootedEntities.add(new RootedEntity(childId, ed));
		}

		if (lazyRootedEntities.isEmpty()) {
			lazyRootedEntities.add(rootedEntity);
		}

		if (ancestorLoadingWorker != null && !ancestorLoadingWorker.isDone()) {
			ancestorLoadingWorker.disregard();
		}

		ancestorLoadingWorker = new SimpleWorker() {

			private List<RootedEntity> ancestors = new ArrayList<RootedEntity>();
				
			protected void doStuff() throws Exception {
				List<String> uniqueIds = EntityUtils.getPathFromUniqueId(contextRootedEntity.getUniqueId());
				List<Long> entityIds = new ArrayList<Long>();
				for(String uniqueId : uniqueIds) {
					entityIds.add(EntityUtils.getEntityIdFromUniqueId(uniqueId));
				}
				Map<Long,Entity>entityMap = EntityUtils.getEntityMap(ModelMgr.getModelMgr().getEntityByIds(entityIds));

				for(String uniqueId : uniqueIds) {
					Long entityId = EntityUtils.getEntityIdFromUniqueId(uniqueId);
					Entity entity = entityMap.get(entityId);
					if (entity!=null) {
						EntityData entityData = new EntityData();
						entityData.setChildEntity(entity);
						ancestors.add(new RootedEntity(uniqueId, entityData));
					}
				}
				
				Collections.reverse(ancestors);
			}

			protected void hadSuccess() {
				setRootedAncestors(ancestors);
				ancestorLoadInProgress.set(false);
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
				
			}
		};
		ancestorLoadingWorker.execute();
		
		loadImageEntities(lazyRootedEntities, success); 
	}

	public void loadImageEntities(final List<RootedEntity> lazyRootedEntities) {
		// TODO: revisit this, since it doesn't set contextRootedEntity
		loadImageEntities(lazyRootedEntities, null);
	}

	private synchronized void loadImageEntities(final List<RootedEntity> lazyRootedEntities, final Callable<Void> success) {

		allRootedEntities = lazyRootedEntities;
		this.numPages = (int)Math.ceil((double)allRootedEntities.size() / (double)PAGE_SIZE);
		
		// Update back/forward navigation
		EntitySelectionHistory history = getEntitySelectionHistory();
		iconDemoToolbar.getPrevButton().setEnabled(history.isBackEnabled());
		iconDemoToolbar.getNextButton().setEnabled(history.isNextEnabled());

		loadImageEntities(0, success);
	}
	
	private synchronized void loadImageEntities(final int pageNum, final Callable<Void> success) {

		if (!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");
		
		this.currPage = pageNum;
		
		int i = pageNum*PAGE_SIZE;
		int j = i+PAGE_SIZE;
		if (j>allRootedEntities.size()) {
			j = allRootedEntities.size();
		}

		final List<RootedEntity> pageEntities = allRootedEntities.subList(i, j);
		
		entityLoadInProgress.set(true);
		annotationLoadInProgress.set(true);
		ancestorLoadInProgress.set(true);
		
		// Indicate a load
		showLoadingIndicator();

		pagingStatusLabel.setText("Page "+(pageNum+1)+" of "+numPages);
		
		
		// Cancel previous loads
		imagesPanel.cancelAllLoads();
		if (entityLoadingWorker != null && !entityLoadingWorker.isDone()) {
			entityLoadingWorker.disregard();
		}
		if (annotationsInitWorker != null && !annotationsInitWorker.isDone()) {
			annotationsInitWorker.disregard();
		}
		
		// Temporarily disable scroll loading
		imagesPanel.setScrollLoadingEnabled(false);
		
		entityLoadingWorker = new SimpleWorker() {

			private List<RootedEntity> loadedRootedEntities = new ArrayList<RootedEntity>();
			
			protected void doStuff() throws Exception {
				for (RootedEntity rootedEntity : pageEntities) {
					if (!EntityUtils.isInitialized(rootedEntity.getEntity())) {
						System.out.println("Warning: had to load entity "+rootedEntity.getEntity().getId());
						rootedEntity.getEntityData().setChildEntity(ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId()+""));
					}
					EntityData defaultImageEd = rootedEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
					if (defaultImageEd!= null && defaultImageEd.getValue() == null && defaultImageEd.getChildEntity()!=null) {
						System.out.println("Warning: had to load default image "+rootedEntity.getEntity().getName());
						defaultImageEd.setChildEntity(ModelMgr.getModelMgr().getEntityById(defaultImageEd.getChildEntity().getId() + ""));
					}
					loadedRootedEntities.add(rootedEntity);
				}
			}

			protected void hadSuccess() {
				setRootedEntities(loadedRootedEntities);
				entityLoadDone(success);
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		entityLoadingWorker.execute();

		annotationsInitWorker = new SimpleWorker() {
			
			@Override
			protected void doStuff() throws Exception {
				List<Long> entityIds = new ArrayList<Long>();
				for(RootedEntity rootedEntity : pageEntities) {
					entityIds.add(rootedEntity.getEntityId());
				}
				annotations.init(entityIds);
			}
			
			@Override
			protected void hadSuccess() {
				
				if (!entityLoadInProgress.get()) {
					System.out.println("Refresh annotations by annotations init worker");
					// Entity load finished before we did, so its safe to update the annotations
					refreshAnnotations(null);
					filterEntities();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							imagesPanel.recalculateGrid();
						}
					});
				}

				annotationLoadInProgress.set(false);
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		annotationsInitWorker.execute();
	}
	
	private synchronized void entityLoadDone(final Callable<Void> success) {
		
		if (!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");

		if (getRootedEntities()==null) {
			System.out.println("Warning: rooted entity list is null upon calling entityLoadDone");
			return;
		}
		
		// Create the image buttons
		imagesPanel.setRootedEntities(getRootedEntities());
		
		// Update preferences for each button
		Boolean invertImages = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
		Boolean tagTable = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY);
		if (invertImages==null) invertImages = false;
        if (tagTable==null) tagTable = false;

		imagesPanel.setTagTable(tagTable);
		imagesPanel.setTagVisbility(iconDemoToolbar.areTagsVisible());
		imagesPanel.setTitleVisbility(iconDemoToolbar.areTitlesVisible());
		imagesPanel.setInvertedColors(invertImages);
		
		// Since the images are not loaded yet, this will just resize the empty
		// buttons so that we can calculate the grid correctly
		imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
		imagesPanel.rescaleImages(imagesPanel.getCurrImageSize());

		// Update selection
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		esm.deselectAll(getSelectionCategory());
		
		// Actually display everything
		showImagePanel();
		updateStatusBar();
		updatePagingStatus();
		
		// Wait until everything is recomputed
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				
				// Select the first entity
				// KR: disabled this because it has unforeseen effects in the split picker. Maybe we can address that in the future.
//				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), getRootedEntities().get(0).getId(), true);

				if (!annotationLoadInProgress.get()) {
					// Annotation load finished before we did, so we have to update the annotations too
					System.out.println("Refresh annotations by entityLoadDone");
					refreshAnnotations(null);
					filterEntities();
				}

				imagesPanel.recalculateGrid();	
				imagesPanel.setScrollLoadingEnabled(true);	
				entityLoadInProgress.set(false);
				
				// Finally, we're done, we can call the success callback
				if (success != null) {
					try {
						success.call();
					} 
					catch (Exception e) {
						SessionMgr.getSessionMgr().handleException(e);
					}
				}
			}
		});
	}

	protected void removeRootedEntity(final RootedEntity rootedEntity) {
		int index = getRootedEntities().indexOf(rootedEntity);
		if (index < 0) return;
		
		pageRootedEntities.remove(rootedEntity);
		pageRootedEntityMap.remove(rootedEntity.getId());
		entityMap.remove(rootedEntity.getEntityId());
		allRootedEntities.remove(rootedEntity);
		
		this.numPages = (int)Math.ceil((double)allRootedEntities.size() / (double)PAGE_SIZE);
		updatePagingStatus();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				RootedEntity next = getNextEntity();
				if (next!=null) {
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), next.getId(), true);	
				}
				imagesPanel.removeRootedEntity(rootedEntity);
				imagesPanel.recalculateGrid();
			}
		});
	}

	private void filterEntities() {

		AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
		if (session == null) {
			return;
		}
		session.clearCompletedIds();
		Set<Long> completed = session.getCompletedEntityIds();
		
		imagesPanel.showAllButtons();
		Boolean hideAnnotated = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY);
		if (hideAnnotated!=null && hideAnnotated) {
			imagesPanel.hideButtons(completed);	
		}
	}

	/**
	 * Reload the annotations from the database and then refresh the UI.
	 */
	public synchronized void reloadAnnotations(final Long entityId) {

		if (annotations == null || pageRootedEntities == null)
			return;

		annotationLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				annotations.reload(entityId);
			}

			protected void hadSuccess() {
				refreshAnnotations(entityId);
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		annotationLoadingWorker.execute();
	}

	/**
	 * Refresh the annotation display in the UI, but do not reload anything from
	 * the database.
	 */
	private synchronized void refreshAnnotations(Long entityId) {
		// Refresh all user list
		allUsers.clear();
		for (OntologyAnnotation annotation : annotations.getAnnotations()) {
			if (!allUsers.contains(annotation.getOwner()))
				allUsers.add(annotation.getOwner());
		}
		Collections.sort(allUsers);
        imagesPanel.setAnnotations(annotations);
        
        if (entityId == null) {
			imagesPanel.showAllAnnotations();
		}
        else {
			imagesPanel.showAnnotationsForEntity(entityId);
		}

	}
	
	@Override
	public void refresh() {
		refresh(null);
	}
	
	public void refresh(final Callable<Void> successCallback) {

		if (contextRootedEntity==null) return;
		
		final List<String> selectedIds = new ArrayList<String>(ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory()));
		final Callable<Void> success = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// At the very end, reselect our buttons if possible
				boolean first = true;
				for(String selectedId : selectedIds) {
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), selectedId, first);
					first = false;
				}
				// Now call the user's callback 
				if (successCallback!=null) successCallback.call();
				return null;
			}
		};
		
		SimpleWorker refreshWorker = new SimpleWorker() {

			RootedEntity rootedEntity = contextRootedEntity;
			
			protected void doStuff() throws Exception {
				Entity entity = ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId()+"");
				if (entity==null) return;
				ModelMgrUtils.loadLazyEntity(entity, false);
				rootedEntity.getEntityData().setChildEntity(entity);
			}

			protected void hadSuccess() {
				if (rootedEntity.getEntity()==null) {
					clear();
					if (success!=null) {
						try {
							success.call();
						}
						catch (Exception e) {
							hadError(e);
						}
					}
				}
				else {
					loadEntity(rootedEntity, success);	
				}
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		refreshWorker.execute();
	}

	public synchronized void clear() {
		this.contextRootedEntity = null;
		this.pageRootedEntities = null;
		this.pageRootedEntityMap = null;
		this.entityMap = null;
		
		setTitle("");
		removeAll();
		add(splashPanel, BorderLayout.CENTER);
		
		revalidate();
		repaint();
	}

	public synchronized void showImagePanel() {

		removeAll();
		add(iconDemoToolbar, BorderLayout.NORTH);
		add(imagesPanel, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		
		revalidate();
		repaint();
	}

	public RootedEntity getPreviousEntity() {
		int i = pageRootedEntities.indexOf(getLastSelectedEntity());
		if (i < 1) {
			// Already at the beginning
			return null;
		}
		return pageRootedEntities.get(i - 1);
	}

	public RootedEntity getNextEntity() {
		int i = pageRootedEntities.indexOf(getLastSelectedEntity());
		if (i > pageRootedEntities.size() - 2) {
			// Already at the end
			return null;
		}
		return pageRootedEntities.get(i + 1);
	}

	public synchronized List<RootedEntity> getRootedEntities() {
		return pageRootedEntities;
	}

	private synchronized void setRootedEntities(List<RootedEntity> rootedEntities) {
		this.pageRootedEntities = rootedEntities;
		this.pageRootedEntityMap = new HashMap<String,RootedEntity>();
		this.entityMap = new HashMap<Long,Entity>();
		
		Set<String> imageRoles = new HashSet<String>();
		for(RootedEntity rootedEntity : rootedEntities) {
			
			pageRootedEntityMap.put(rootedEntity.getId(), rootedEntity);
			entityMap.put(rootedEntity.getEntity().getId(), rootedEntity.getEntity());
			
			for(EntityData ed : rootedEntity.getEntity().getEntityData()) {
				if (EntityUtils.hasImageRole(ed)) {
					imageRoles.add(ed.getEntityAttribute().getName());
				}
			}
		}
		
		allImageRoles.clear();
		allImageRoles.addAll(imageRoles);
		Collections.sort(allImageRoles);
		
		iconDemoToolbar.getImageRoleButton().setEnabled(!allImageRoles.isEmpty());
		if (!allImageRoles.contains(currImageRole)) {
			currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;
		}
	}
	
	private synchronized void setRootedAncestors(List<RootedEntity> rootedAncestors) {
		this.rootedAncestors = rootedAncestors;
		StringBuffer buf = new StringBuffer();
		for(int i=rootedAncestors.size()-1; i>=0; i--) {
			RootedEntity ancestor = rootedAncestors.get(i);
			if (buf.length()>0) buf.append(" : ");
			buf.append(ancestor.getEntity().getName());
		}
		setTitle(buf.toString());
	}

	public synchronized RootedEntity getLastSelectedEntity() {
		String entityId = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(getSelectionCategory());
		if (entityId == null) return null;
		AnnotatedImageButton button = imagesPanel.getButtonById(entityId);
		if (button == null) return null;
		return button.getRootedEntity();
	}

	public synchronized List<RootedEntity> getSelectedEntities() {
		List<RootedEntity> selectedEntities = new ArrayList<RootedEntity>();
		if (pageRootedEntities==null) return selectedEntities;
		for(RootedEntity rootedEntity : pageRootedEntities) {
			AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
			if (button.isSelected()) {
				selectedEntities.add(rootedEntity);
			}
		}
		return selectedEntities;
	}
	
	public String getCurrImageRole() {
		return currImageRole;
	}

	public ImagesPanel getImagesPanel() {
		return imagesPanel;
	}

	public IconDemoToolbar getToolbar() {
		return iconDemoToolbar;
	}

	public Hud getHud() {
		return hud;
	}
	
	public Annotations getAnnotations() {
		return annotations;
	}

	public RootedEntity getContextRootedEntity() {
		return contextRootedEntity;
	}

	private List<RootedEntity> getRootedEntitiesById(String id) {
		List<RootedEntity> res = new ArrayList<RootedEntity>();
		if (pageRootedEntityMap==null) return res;
		RootedEntity re = pageRootedEntityMap.get(id);
		if (re!=null) {
			res.add(re);
		}
		else {
			for(RootedEntity rootedEntity : pageRootedEntities) {
				if (rootedEntity.getEntity().getId().toString().equals(id)) {
					res.add(rootedEntity);	
				}
			}	
		}
		return res;
	}
	
	@Override
	public RootedEntity getRootedEntityById(String id) {
		if (pageRootedEntityMap==null) return null;
		return pageRootedEntityMap.get(id);
	}
	
	@Override	
	public Entity getEntityById(String id) {
		if (entityMap==null && pageRootedEntityMap==null) return null;
		try {
			return entityMap.get(new Long(id));	
		}
		catch (Exception e) {
			RootedEntity re = pageRootedEntityMap.get(id);
			if (re!=null) return re.getEntity();
		}
		return null;
	}
}
