/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseForwarder;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This panel shows images for annotation. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends Viewer {

	private SplashPanel splashPanel;
	private JToolBar toolbar;
	private JButton prevButton;
	private JButton nextButton;
	private JButton parentButton;
	private JToggleButton showTitlesButton;
	private JButton imageRoleButton;
	private JToggleButton showTagsButton;
	private JButton userButton;
	private JSlider imageSizeSlider;
	private ImagesPanel imagesPanel;
	private JPanel statusBar;
	private JLabel statusLabel;
	private Hud hud;
	
	// Category within the selection model
	private String selectionCategory;

	// The parent entity which we are displaying children for
	private EntityData contextEntityData;
	private String contextUniqueId;
	
	// Children of the parent entity
	private List<EntityData> entityDatas;
	private	List<Entity> entities;
		
	private int currImageSize;
	private int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
	
	private final List<String> allUsers = new ArrayList<String>();
	private final Set<String> hiddenUsers = new HashSet<String>();
	private final Annotations annotations = new Annotations();

	private final List<String> allImageRoles = new ArrayList<String>();
	private String currImageRole;
	
	private SimpleWorker entityLoadingWorker;
	private SimpleWorker annotationLoadingWorker;

	// Listen for key strokes and execute the appropriate key bindings
	private final KeyListener keyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {

			if (KeymapUtil.isModifier(e))
				return;
			if (e.getID() != KeyEvent.KEY_PRESSED)
				return;

			KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
			if (!SessionMgr.getKeyBindings().executeBinding(shortcut)) {

				// No keybinds matched, use the default behavior

				// Ctrl-A or Meta-A to select all
				if (e.getKeyCode() == KeyEvent.VK_A && ((SystemInfo.isMac && e.isMetaDown()) || (e.isControlDown()))) {
					for (EntityData entityData : entityDatas) {
						Entity entity = entityData.getChildEntity(); 
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(selectionCategory, entity.getId()+"", false);
					}
					return;
				}
				
				// Space on a single entity triggers a preview 
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					updateHud();
					hud.showDialog();
					e.consume();
					return;
				}

				// Enter with a single entity selected triggers an outline
				// navigation
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(selectionCategory);
					if (selectedIds.size() != 1)
						return;
					Long selectedId = new Long(selectedIds.get(0));
					EntityData selectedEd = getEntityDataWithEntityId(selectedId);
					String uniqueId = EntityOutline.getChildUniqueId(contextUniqueId, selectedEd);
					if (uniqueId != null) {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId, true);
					}
					return;
				}

				// Tab and arrow navigation to page through the images
				boolean clearAll = false;
				Entity entity = null;
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					clearAll = true;
					if (e.isShiftDown()) {
						entity = getPreviousEntity();
					} else {
						entity = getNextEntity();
					}
				} else {
					clearAll = true;
					if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						entity = getPreviousEntity();
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						entity = getNextEntity();
					}
				}

				if (entity != null) {
					AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entity.getId());
					if (button != null) {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(selectionCategory, button.getEntity().getId()+"", clearAll);
						imagesPanel.scrollEntityToCenter(entity);
					}
				}
			}

			revalidate();
			repaint();

		}
	};

	public IconDemoPanel(final ViewerSplitPanel viewerContainer, final String selectionCategory) {

		super(viewerContainer);
		
		this.selectionCategory = selectionCategory;
		
		currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;
		
		setBorder(BorderFactory.createEmptyBorder());
		setBackground(Color.white);
		setLayout(new BorderLayout());
		setFocusable(true);

		hud = new Hud();

		hud.getJDialog().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					hud.hideDialog();
				}
				else {
					Entity entity = null;
					if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_UP) {
						entity = getPreviousEntity();
					} 
					else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_DOWN) {
						entity = getNextEntity();
					}
					
					if (entity==null) {
						hud.hideDialog();
						return;
					}
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(selectionCategory, entity.getId()+"", true);
					updateHud();
				}
			}
		});
		
		splashPanel = new SplashPanel();
		add(splashPanel);

		toolbar = createToolbar();
		imagesPanel = new ImagesPanel(this);
		imagesPanel.setButtonKeyListener(keyListener);
		imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconDemoPanel"));
		toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->IconDemoPanel"));
		
		statusBar = new JPanel();
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
		statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color)UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
		
		statusBar.add(Box.createRigidArea(new Dimension(10,20)));
        statusLabel = new JLabel("");
        statusBar.add(statusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10,20)));
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				setAsActive();
			}
		});
		
		imageSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int imageSize = source.getValue();
				if (currImageSize == imageSize) return;
				currImageSize = imageSize;
				imagesPanel.rescaleImages(imageSize);
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
						if (button != null)
							reloadAnnotations(button.getEntity());
						filterEntities();
					}
				});
			}

			@Override
			public void entitySelected(String category, String entityId, boolean clearAll) {
				if (category.equals(selectionCategory)) {
					imagesPanel.setSelection(new Long(entityId), true, clearAll);
					updateHud();
					updateStatusBar();
				}
			}

			@Override
			public void entityDeselected(String category, String entityId) {
				if (category.equals(selectionCategory)) {
					imagesPanel.setSelection(new Long(entityId), false, false);
					updateHud();
					updateStatusBar();
				}
			}

			@Override
			public void entityChanged(final long entityId) {
				if (contextEntityData==null) return;
				SimpleWorker worker = new SimpleWorker() {
					@Override
					protected void doStuff() throws Exception {
						Entity newEntity = ModelMgr.getModelMgr().getEntityById(entityId+"");
						
						if (contextEntityData.getChildEntity().getId().equals(entityId)) {
							if (newEntity==null) {
								goParent();
							}
							else {
								refresh();	
							}
							return;
						}
						
						boolean refreshAll = false;
						AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
						if (button != null) {
							EntityData entityData = button.getEntityData();
							if (entityData != null) {
								if (newEntity==null) {
									refreshAll = true;
								}
								else {
									ModelMgrUtils.updateEntity(button.getEntity(), newEntity);	
									button.refresh(entityData);
									button.setViewable(true);
								}
							}
						}
						if (refreshAll) {
							refresh();	
						}
					}
					
					@Override
					protected void hadSuccess() {
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
				Boolean onlySession = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
						ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY);
				if (!onlySession || session == null) return true;
				
				// At this point we know there is a current session, and we have
				// to match it
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
					imagesPanel.setTagTable((Boolean)newValue);
					imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
					imagesPanel.rescaleImages(imagesPanel.getCurrImageSize());
					imagesPanel.recalculateGrid();
					imagesPanel.loadUnloadImages();
				}
				else if (ViewerSettingsPanel.ANNOTATION_TABLES_HEIGHT_PROPERTY.equals(key)) {
					int tableHeight = (Integer)newValue;
					if (currTableHeight == tableHeight) return;
					currTableHeight = tableHeight;
					imagesPanel.resizeTables(tableHeight);
					imagesPanel.rescaleImages(currImageSize);
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

	private void updateStatusBar() {
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		int s = esm.getSelectedEntitiesIds(selectionCategory).size();
		statusLabel.setText(s+" of "+entityDatas.size()+" selected");
	}
	
	private void updateHud() {
		List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(selectionCategory);
		if (selectedIds.size() != 1) {
			hud.hideDialog();
			return;
		}
		Long selectedId = new Long(selectedIds.get(0));
		AnnotatedImageButton button = imagesPanel.getButtonByEntityId(selectedId);
		if (button instanceof DynamicImageButton) {
			DynamicImageButton d = (DynamicImageButton)button;
			BufferedImage bufferedImage = d.getDynamicImagePanel().getMaxSizeImage();
			if (bufferedImage==null) {
				return;
			}
			hud.setTitle(button.getEntity().getName());
			hud.setImage(bufferedImage);
		}
	}
	
	private synchronized void goBack() {
		EntityOutlineHistory history = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutlineHistory();
		history.goBack();
	}

	private synchronized void goForward() {
		EntityOutlineHistory history = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutlineHistory();
		history.goForward();
	}

	private synchronized void goParent() {
		String selectedUniqueId = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(EntitySelectionModel.CATEGORY_OUTLINE);
		if (Utils.isEmpty(selectedUniqueId)) return;
		ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, Utils.getParentIdFromUniqueId(selectedUniqueId), true);
	}

	private boolean isParentEnabled() {
		String selectedUniqueId = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(EntitySelectionModel.CATEGORY_OUTLINE);
		return (!Utils.isEmpty(Utils.getParentIdFromUniqueId(selectedUniqueId)));
	}
	
	private JToolBar createToolbar() {

		JToolBar toolBar = new JToolBar("Still draggable");
		toolBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, (Color)UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
		toolBar.setFloatable(true);
		toolBar.setRollover(true);

		prevButton = new JButton();
		prevButton.setIcon(Icons.getIcon("arrow_back.gif"));
		prevButton.setToolTipText("Go back in your browsing history");
		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goBack();
			}
		});
		toolBar.add(prevButton);

		nextButton = new JButton();
		nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
		nextButton.setToolTipText("Go forward in your browsing history");
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goForward();
			}
		});
		toolBar.add(nextButton);

		parentButton = new JButton();
		parentButton.setIcon(Icons.getIcon("parent.gif"));
		parentButton.setToolTipText("Go to the parent entity");
		parentButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goParent();
			}
		});
		toolBar.add(parentButton);

		toolBar.addSeparator();

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
				imagesPanel.loadUnloadImages();
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
				imagesPanel.loadUnloadImages();
			}
		});
		toolBar.add(showTagsButton);

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
		
		imageRoleButton = new JButton("Image type...");
		imageRoleButton.setIcon(Icons.getIcon("image.png"));
		imageRoleButton.setFocusable(false);
		imageRoleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupImageRoleMenu();
			}
		});
		toolBar.add(imageRoleButton);

		toolBar.addSeparator();

		imageSizeSlider = new JSlider(ImagesPanel.MIN_THUMBNAIL_SIZE, ImagesPanel.MAX_THUMBNAIL_SIZE,
				ImagesPanel.DEFAULT_THUMBNAIL_SIZE);
		imageSizeSlider.setFocusable(false);
		imageSizeSlider.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
		imageSizeSlider.setToolTipText("Image size percentage");
		toolBar.add(imageSizeSlider);

		return toolBar;
	}

	private void showPopupImageRoleMenu() {

		final JPopupMenu imageRoleListMenu = new JPopupMenu();
		final List<String> imageRoles = new ArrayList<String>(allImageRoles);

		for (final String imageRole : imageRoles) {
			JMenuItem roleMenuItem = new JCheckBoxMenuItem(imageRole, imageRole.equals(currImageRole));
			roleMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					currImageRole = imageRole;
					entityLoadDone();
				}
			});
			imageRoleListMenu.add(roleMenuItem);
		}

		imageRoleListMenu.show(imageRoleButton, 0, imageRoleButton.getHeight());
	}
	
	private void showPopupUserMenu() {

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

		userListMenu.show(userButton, 0, userButton.getHeight());
	}
	
	public void showLoadingIndicator() {
		removeAll();
		add(new JLabel(Icons.getLoadingIcon()));
		this.updateUI();
	}

	public boolean areTitlesVisible() {
		return showTitlesButton.isSelected();
	}

	public boolean areTagsVisible() {
		return showTagsButton.isSelected();
	}
	
	public synchronized void loadEntity(EntityData entityData, String uniqueId) {
		
		this.contextEntityData = entityData;
		this.contextUniqueId = uniqueId;
		Entity entity = contextEntityData.getChildEntity();

		setTitle(entity.getName());
		
		List<EntityData> eds = entity.getOrderedEntityData();
		List<EntityData> children = new ArrayList<EntityData>();
		for(EntityData ed : eds) {
			Entity child = ed.getChildEntity();
			if (!EntityUtils.isHidden(ed) && child!=null) {
				children.add(ed);
			}
		}
		if (children.isEmpty()) {
			children = new ArrayList<EntityData>();
			children.add(entityData);
		}
		loadImageEntities(children); 
	}

	public void loadImageEntities(final List<EntityData> entityDatas) {
		loadImageEntities(entityDatas, null);
	}

	private synchronized void loadImageEntities(final List<EntityData> entityDatas, final Callable<Void> success) {

		// Indicate a load
		showLoadingIndicator();
		
		// Cancel previous loads

		if (entityLoadingWorker != null && !entityLoadingWorker.isDone()) {
			System.out.println("Cancel previous image load");
			entityLoadingWorker.disregard();
		}
		imagesPanel.cancelAllLoads();

		// Update back/forward navigation
		EntityOutlineHistory history = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutlineHistory();
		prevButton.setEnabled(history.isBackEnabled());
		nextButton.setEnabled(history.isNextEnabled());
		parentButton.setEnabled(isParentEnabled());

		// Temporarily disable scroll loading
		imagesPanel.setScrollLoadingEnabled(false);
		
		entityLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				List<EntityData> loadedEntityDatas = new ArrayList<EntityData>();
				for (EntityData entityData : entityDatas) {
					if (!EntityUtils.isInitialized(entityData.getChildEntity())) {
						entityData.setChildEntity(ModelMgr.getModelMgr().getEntityById(entityData.getChildEntity().getId()+""));
					}
					EntityData defaultImageEd = entityData.getChildEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
					if (defaultImageEd!= null && defaultImageEd.getValue() == null && defaultImageEd.getChildEntity()!=null) {
						defaultImageEd.setChildEntity(ModelMgr.getModelMgr().getEntityById(defaultImageEd.getChildEntity().getId() + ""));
					}
					loadedEntityDatas.add(entityData);
				}
				setEntityDatas(loadedEntityDatas);
				annotations.init(entities);
			}

			protected void hadSuccess() {
				entityLoadDone();
				try {
					if (success != null)
						success.call();
				} catch (Exception e) {
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
		
		if (!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");

		imagesPanel.setEntityDatas(getEntityDatas());
		refreshAnnotations(null);

		showAllEntities();
		filterEntities();

		Boolean invertImages = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
		Boolean tagTable = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY);
		
		imagesPanel.setTagTable(tagTable);
		imagesPanel.setTagVisbility(showTagsButton.isSelected());
		imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
		imagesPanel.setInvertedColors(invertImages);
		
		// Since the images are not loaded yet, this will just resize the empty
		// buttons so that we can calculate the grid correctly
		imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
		imagesPanel.rescaleImages(imagesPanel.getCurrImageSize());
		imagesPanel.recalculateGrid();
		
		revalidate();
		repaint();

		// Wait until everything is recomputed
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				imagesPanel.setScrollLoadingEnabled(true);
				imagesPanel.loadUnloadImages();
				// Select the first entity
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(selectionCategory, entityDatas.get(0).getId()+"", true);
			}
		});
	}

	private synchronized void entityLoadError(Throwable error) {

		error.printStackTrace();
		if (getEntityDatas() != null) {
			JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error",
					JOptionPane.ERROR_MESSAGE);
			imagesPanel.setEntityDatas(getEntityDatas());
			showAllEntities();
			// TODO: set read-only mode
		} else {
			JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading session", "Data Loading Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void filterEntities() {

		AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
		if (session == null)
			return;
		session.clearCompletedIds();
		Set<Long> completed = session.getCompletedEntityIds();
		
		imagesPanel.showAllButtons();
		Boolean hideAnnotated = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY);
		if (hideAnnotated) {
			imagesPanel.hideButtons(completed);	
		}
	}

	/**
	 * Reload the annotations from the database and then refresh the UI.
	 */
	public synchronized void reloadAnnotations() {

		if (annotations == null || entityDatas == null)
			return;

		annotationLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				annotations.init(entities);
			}

			protected void hadSuccess() {
				refreshAnnotations(null);
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error",
						JOptionPane.ERROR_MESSAGE);
			}
		};

		annotationLoadingWorker.execute();
	}

	/**
	 * Reload the annotations from the database and then refresh the UI.
	 */
	public synchronized void reloadAnnotations(final Entity entity) {

		if (annotations == null || entityDatas == null)
			return;

		annotationLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				annotations.reload(entity);
			}

			protected void hadSuccess() {
				refreshAnnotations(entity);
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error",
						JOptionPane.ERROR_MESSAGE);
			}
		};

		annotationLoadingWorker.execute();
	}

	/**
	 * Refresh the annotation display in the UI, but do not reload anything from
	 * the database.
	 */
	private synchronized void refreshAnnotations(Entity entity) {

		// Refresh all user list
		allUsers.clear();
		for (OntologyAnnotation annotation : annotations.getAnnotations()) {
			if (!allUsers.contains(annotation.getOwner()))
				allUsers.add(annotation.getOwner());
		}
		Collections.sort(allUsers);

		if (entity == null) {
			imagesPanel.loadAnnotations(annotations);
		} else {
			imagesPanel.loadAnnotations(annotations, entity);
		}

	}
	
	@Override
	public void refresh() {

		SimpleWorker refreshWorker = new SimpleWorker() {

			EntityData entityData = contextEntityData;
			
			protected void doStuff() throws Exception {
				entityData.setChildEntity(ModelMgr.getModelMgr().getEntityById(entityData.getChildEntity().getId()+""));
			}

			protected void hadSuccess() {
				if (entityData.getChildEntity()==null) {
					clear();
				}
				else {
					loadEntity(contextEntityData, contextUniqueId);	
				}
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(IconDemoPanel.this, "Error refreshing", "Data Loading Error",
						JOptionPane.ERROR_MESSAGE);
			}
		};

		refreshWorker.execute();
	}

	public void clear() {
		this.contextEntityData = null;
		this.contextUniqueId = null;
		this.entities = null;
		this.entityDatas = null;
		setTitle("");
		removeAll();
		add(splashPanel, BorderLayout.CENTER);
	}

	public synchronized void showAllEntities() {

		removeAll();
		add(toolbar, BorderLayout.NORTH);
		add(imagesPanel, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		
		revalidate();
		repaint();

		// Focus on the panel so that it can receive keyboard input
		requestFocusInWindow();
	}

	public Entity getPreviousEntity() {
		List<Entity> entities = getEntities();
		int i = entities.indexOf(getLastSelectedEntity());
		if (i < 1) {
			// Already at the beginning
			return null;
		}
		return entities.get(i - 1);
	}

	public Entity getNextEntity() {
		List<Entity> entities = getEntities();
		int i = entities.indexOf(getLastSelectedEntity());
		if (i > entities.size() - 2) {
			// Already at the end
			return null;
		}
		return entities.get(i + 1);
	}

	public synchronized List<EntityData> getEntityDatas() {
		return entityDatas;
	}

	public synchronized List<Entity> getEntities() {
		return entities;
	}

	private synchronized void setEntityDatas(List<EntityData> entityDatas) {
		this.entityDatas = entityDatas;
		this.entities = new ArrayList<Entity>();

		for(EntityData entityData : entityDatas) {
			entities.add(entityData.getChildEntity());
		}
		
		Set<String> imageRoles = new HashSet<String>();
		for(EntityData entityData : entityDatas) {
			Entity entity = entityData.getChildEntity();
			for(EntityData ed : entity.getEntityData()) {
				String attrName = ed.getEntityAttribute().getName();
				if (attrName.endsWith("Image")) {
					imageRoles.add(attrName);
				}
			}
		}
		
		allImageRoles.clear();
		allImageRoles.addAll(imageRoles);
		Collections.sort(allImageRoles);
		
		imageRoleButton.setEnabled(!allImageRoles.isEmpty());
	}

	public synchronized Entity getLastSelectedEntity() {
		String entityId = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(selectionCategory);
		if (entityId == null)
			return null;
		AnnotatedImageButton button = imagesPanel.getButtonByEntityId(new Long(entityId));
		if (button == null)
			return null;
		return button.getEntity();
	}

	public synchronized List<Entity> getSelectedEntities() {
		List<Entity> selectedEntities = new ArrayList<Entity>();
		if (entityDatas==null) return selectedEntities;
		for (EntityData entityData : entityDatas) {
			Entity entity = entityData.getChildEntity();
			AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entity.getId());
			if (button.isSelected())
				selectedEntities.add(entity);
		}
		return selectedEntities;
	}
	
	public EntityData getEntityDataWithEntityId(Long entityId) {
		for(EntityData ed : entityDatas) {
			if (ed.getChildEntity().getId().equals(entityId)) {
				return ed;
			}
		}
		return null;
	}

	public String getCurrImageRole() {
		return currImageRole;
	}

	public ImagesPanel getImagesPanel() {
		return imagesPanel;
	}

	public JSlider getImageSizeSlider() {
		return imageSizeSlider;
	}

	public KeyListener getKeyListener() {
		return keyListener;
	}

	public double getCurrImageSizePercent() {
		return currImageSize;
	}

	public Annotations getAnnotations() {
		return annotations;
	}

	public String getSelectionCategory() {
		return selectionCategory;
	}

	public EntityData getContextEntityData() {
		return contextEntityData;
	}

	public String getContextUniqueId() {
		return contextUniqueId;
	}
}
