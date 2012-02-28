/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

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
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityOutlineHistory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This panel shows images for annotation. It may show a bunch of images at
 * once, or a single image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends JPanel {

	private SplashPanel splashPanel;
	private JToolBar toolbar;
	private JButton prevButton;
	private JButton nextButton;
	private JButton parentButton;
	private JToggleButton showTitlesButton;
	private JButton imageRoleButton;
	private JToggleButton showTagsButton;
	private JToggleButton invertButton;
	private JToggleButton hideCompletedButton;
	private JToggleButton onlySessionButton;
	private JButton userButton;
	private JToggleButton tagTableButton;
	private JSlider tagTableSlider;
	private JSlider imageSizeSlider;
	
	private ImagesPanel imagesPanel;
	private AnnotationDetailsDialog annotationDetailsDialog;

	private Entity entity;
	private List<Entity> entities;
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
					for (Entity entity : entities) {
						ModelMgr.getModelMgr().selectEntity(entity.getId(), false);
					}
					return;
				}

				// Enter with a single entity selected triggers an outline
				// navigation
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					List<Long> selectedIds = ModelMgr.getModelMgr().getSelectedEntitiesIds();
					if (selectedIds.size() != 1)
						return;
					Long selectedId = selectedIds.get(0);
					String uniqueId = SessionMgr.getSessionMgr().getActiveBrowser().getEntityOutline()
							.getChildUniqueIdWithEntity(selectedId);
					if (uniqueId != null) {
						ModelMgr.getModelMgr().selectOutlineEntity(uniqueId, true);
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
						ModelMgr.getModelMgr().selectEntity(button.getEntity().getId(), clearAll);
						imagesPanel.scrollEntityToCenter(entity);
					}
				}
			}

			revalidate();
			repaint();

		}
	};


	public IconDemoPanel() {

		currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;
		
		setBackground(Color.white);
		setLayout(new BorderLayout());
		setFocusable(true);

		splashPanel = new SplashPanel();
		add(splashPanel);

		toolbar = createToolbar();
		imagesPanel = new ImagesPanel();
		imagesPanel.setButtonKeyListener(keyListener);

		annotationDetailsDialog = new AnnotationDetailsDialog();

		imageSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int imageSize = source.getValue();
				if (currImageSize == imageSize)
					return;
				currImageSize = imageSize;
				imagesPanel.rescaleImages(imageSize);
				imagesPanel.recalculateGrid();
				imagesPanel.loadUnloadImages();
			}
		});
		
		tagTableSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int tableHeight = source.getValue();
				if (currTableHeight == tableHeight) return;
				currTableHeight = tableHeight;
				imagesPanel.resizeTables(tableHeight);
				imagesPanel.rescaleImages(currImageSize);
				imagesPanel.recalculateGrid();
				imagesPanel.scrollSelectedEntitiesToCenter();
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
			public void entitySelected(long entityId, boolean clearAll) {
				imagesPanel.setSelection(entityId, true, clearAll);
				return;
			}

			@Override
			public void entityDeselected(long entityId) {
				imagesPanel.setSelection(entityId, false, false);
			}

			@Override
			public void entityChanged(long entityId) {
				AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
				if (button != null) {
					Entity entity = button.getEntity();
					if (entity != null) {
						ModelMgrUtils.updateEntity(entity);
						button.refresh(entity);
						button.setViewable(true);
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
				if (hiddenUsers.contains(annotation.getOwner()))
					return false;
				AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
				// Hidden by session?
				if (!onlySessionButton.isSelected() || session == null)
					return true;
				// At this point we know there is a current session, and we have
				// to match it
				return (annotation.getSessionId() != null && annotation.getSessionId().equals(session.getId()));
			}
		});

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
		String selectedUniqueId = ModelMgr.getModelMgr().getLastSelectedOutlineEntityId();
		if (Utils.isEmpty(selectedUniqueId)) return;
		ModelMgr.getModelMgr().selectOutlineEntity(Utils.getParentIdFromUniqueId(selectedUniqueId), true);
	}

	private boolean isParentEnabled() {
		String selectedUniqueId = ModelMgr.getModelMgr().getLastSelectedOutlineEntityId();
		return (!Utils.isEmpty(Utils.getParentIdFromUniqueId(selectedUniqueId)));
	}
	
	private JToolBar createToolbar() {

		JToolBar toolBar = new JToolBar("Still draggable");
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
				} finally {
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
		hideCompletedButton
				.setToolTipText("Hide images which have been annotated completely according to the annotation session's ruleset.");
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
		
		tagTableButton = new JToggleButton();
		tagTableButton.setIcon(Icons.getIcon("table.png"));
		tagTableButton.setFocusable(false);
		tagTableButton.setToolTipText("Show annotations in a table instead of a tag cloud");
		tagTableButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tagTableSlider.setEnabled(tagTableButton.isSelected());
				imagesPanel.setTagTable(tagTableButton.isSelected());
				imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
				imagesPanel.rescaleImages(imagesPanel.getCurrImageSize());
				imagesPanel.recalculateGrid();
				imagesPanel.loadUnloadImages();
			}
		});
		toolBar.add(tagTableButton);

		tagTableSlider = new JSlider(ImagesPanel.MIN_TABLE_HEIGHT, ImagesPanel.MAX_TABLE_HEIGHT,
				ImagesPanel.DEFAULT_TABLE_HEIGHT);
		tagTableSlider.setFocusable(false);
		tagTableSlider.setEnabled(false);
		tagTableSlider.setToolTipText("Tag table height");
		toolBar.add(tagTableSlider);
		
		toolBar.addSeparator();

		imageSizeSlider = new JSlider(ImagesPanel.MIN_THUMBNAIL_SIZE, ImagesPanel.MAX_THUMBNAIL_SIZE,
				ImagesPanel.DEFAULT_THUMBNAIL_SIZE);
		imageSizeSlider.setFocusable(false);
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

	public synchronized void loadEntity(Entity parentEntity) {
		this.entity = parentEntity;
		List<EntityData> eds = parentEntity.getOrderedEntityData();
		List<Entity> children = new ArrayList<Entity>();
		for(EntityData ed : eds) {
			Entity child = ed.getChildEntity();
			if (!EntityUtils.isHidden(ed) && child!=null) {
				children.add(child);
			}
		}
		if (children.isEmpty()) {
			children = new ArrayList<Entity>();
			children.add(parentEntity);
		}
		loadImageEntities(children); 
	}

	public void loadImageEntities(final List<Entity> entities) {
		loadImageEntities(entities, null);
	}

	private synchronized void loadImageEntities(final List<Entity> entities, final Callable<Void> success) {

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
				List<Entity> loadedEntities = new ArrayList<Entity>();
				for (Entity entity : entities) {
					EntityData ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
					if (ed!= null && ed.getValue() == null && ed.getChildEntity()!=null) {
						ed.setChildEntity(ModelMgr.getModelMgr().getEntityById(ed.getChildEntity().getId() + ""));
					}

					loadedEntities.add(entity);
				}
				setEntities(loadedEntities);
				annotations.init(loadedEntities);
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

		imagesPanel.setEntities(getEntities());
		refreshAnnotations(null);

		showAllEntities();
		filterEntities();

		imagesPanel.setTagTable(tagTableButton.isSelected());
		imagesPanel.setTagVisbility(showTagsButton.isSelected());
		imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
		imagesPanel.setInvertedColors(invertButton.isSelected());
		
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
				ModelMgr.getModelMgr().selectEntity(entities.get(0).getId(), true);
			}
		});
	}

	private synchronized void entityLoadError(Throwable error) {

		error.printStackTrace();
		if (getEntities() != null) {
			JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error",
					JOptionPane.ERROR_MESSAGE);
			imagesPanel.setEntities(getEntities());
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

		for (AnnotatedImageButton button : imagesPanel.getButtons().values()) {
			if (hideCompletedButton.isSelected() && completed.contains(button.getEntity().getId())) {
				button.setVisible(false);
			} else {
				button.setVisible(true);
			}
		}
	}

	/**
	 * Reload the annotations from the database and then refresh the UI.
	 */
	public synchronized void reloadAnnotations() {

		if (annotations == null || entities == null)
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

		if (annotations == null || entities == null)
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
	
	public synchronized void clear() {
		this.entities = null;
		removeAll();
		add(splashPanel, BorderLayout.CENTER);

		revalidate();
		repaint();
	}

	public synchronized void showAllEntities() {

		removeAll();
		add(toolbar, BorderLayout.NORTH);
		add(imagesPanel, BorderLayout.CENTER);

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

	public synchronized List<Entity> getEntities() {
		return entities;
	}

	private synchronized void setEntities(List<Entity> entities) {
		this.entities = entities;
		
		Set<String> imageRoles = new HashSet<String>();
		for(Entity entity : entities) {
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
		Long entityId = ModelMgr.getModelMgr().getLastSelectedEntityId();
		if (entityId == null)
			return null;
		AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entityId);
		if (button == null)
			return null;
		return button.getEntity();
	}

	public synchronized List<Entity> getSelectedEntities() {
		List<Entity> selectedEntities = new ArrayList<Entity>();
		if (entities==null) return selectedEntities;
		for (Entity entity : entities) {
			AnnotatedImageButton button = imagesPanel.getButtonByEntityId(entity.getId());
			if (button.isSelected())
				selectedEntities.add(entity);
		}
		return selectedEntities;
	}

	public String getCurrImageRole() {
		return currImageRole;
	}

	public ImagesPanel getImagesPanel() {
		return imagesPanel;
	}

	public JSlider getTagTableSlider() {
		return tagTableSlider;
	}

	public JSlider getImageSizeSlider() {
		return imageSizeSlider;
	}

	public KeyListener getKeyListener() {
		return keyListener;
	}

	public boolean isInverted() {
		return invertButton.isSelected();
	}

	public double getCurrImageSizePercent() {
		return currImageSize;
	}

	public void viewAnnotationDetails(OntologyAnnotation tag) {
		annotationDetailsDialog.showForAnnotation(tag);
	}

	public Annotations getAnnotations() {
		return annotations;
	}
}
