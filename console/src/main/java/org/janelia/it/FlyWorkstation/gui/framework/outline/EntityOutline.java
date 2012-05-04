/*
 * Created by IntelliJ IDEA. 
 * User: saffordt 
 * Date: 2/8/11 
 * Time: 2:09 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.EntityDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.*;

/**
 * The entity tree which lives in the right-hand "Data" panel and drives the IconDemoPanel.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityOutline extends EntityTree implements Cloneable, Refreshable {

	private String currUniqueId;

	public EntityOutline() {
		super(true);
		this.setMinimumSize(new Dimension(400, 400));
		showLoadingIndicator();

		ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

			@Override
			public void entityOutlineSelected(String entityPath, boolean clearAll) {
				selectEntityByUniqueId(entityPath);
			}

			@Override
			public void entityOutlineDeselected(String entityPath) {
				getTree().clearSelection();
			}

			@Override
			public void entityChanged(long entityId) {
				// Update all the entities that are affected
				for (Entity entity : getEntitiesById(entityId)) {
					ModelMgrUtils.updateEntity(entity);
					revalidate();
					repaint();
				}
			}
		});
	}

	public void init(List<Entity> entityRootList) {

		EntityType rootType = new EntityType();
		rootType.setName("");
		rootType.setAttributes(new HashSet<EntityAttribute>());
		
		Entity root = new Entity();
		root.setEntityType(rootType);
		root.setName("Data");
		
		if (null != entityRootList && entityRootList.size() >= 1) {
			root.setEntityType(entityRootList.get(0).getEntityType());
			
			for (Entity commonRoot : entityRootList) {
				addTopLevelEntity(root, commonRoot);
			}

			initializeTree(root);
		} 
		else {
			EntityType type = new EntityType();
			type.setName("");
			type.setAttributes(new HashSet<EntityAttribute>());
			
			Entity noDataEntity = new Entity();
			noDataEntity.setEntityType(type);
			noDataEntity.setName("No data");

			addTopLevelEntity(root, noDataEntity);
			
			initializeTree(root);
		}
	}

	private EntityData addTopLevelEntity(Entity rootEntity, Entity entity) {
		EntityData ed = rootEntity.addChildEntity(entity);
		ed.setOrderIndex(rootEntity.getMaxOrderIndex() + 1);
		ed.setUser(entity.getUser());
		return ed;
	}

	@Override
	public void initializeTree(Entity rootEntity) {
		super.initializeTree(rootEntity);
		
		selectedTree.expand(selectedTree.getRootNode(), true);
		
		JTree tree = getTree();
		tree.setRootVisible(false);
		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.ON_OR_INSERT);
		tree.setTransferHandler(new EntityTransferHandler() {
			@Override
			public JComponent getDropTargetComponent() {
				return EntityOutline.this;
			}
		});
	}
	
	/**
	 * Override this method to load the root list. This method will be called in
	 * a worker thread.
	 * 
	 * @return
	 */
	public abstract List<Entity> loadRootList();

	private class EntityOutlineContextMenu extends EntityContextMenu {

		private DefaultMutableTreeNode node;
		private EntityData entityData;

		public EntityOutlineContextMenu(DefaultMutableTreeNode node) {
			super(getEntity(node));
			this.entityData = getEntityData(node);
			this.node = node;
		}

		public void addMenuItems() {

			add(getTitleItem());
			add(getCopyNameToClipboardItem());
			add(getCopyIdToClipboardItem());
			add(getDetailsItem());
			add(getRenameItem());
			add(getDeleteItem());
			add(getNewFolderItem());

			setNextAddRequiresSeparator(true);
			add(getOpenInSecondViewerItem());
			add(getOpenInFinderItem());
			add(getOpenWithAppItem());
			add(getNeuronAnnotatorItem());
			add(getVaa3dItem());

			setNextAddRequiresSeparator(true);
			add(getCreateSessionItem());
			
	        setNextAddRequiresSeparator(true);
	        add(getSearchHereItem());
		}

		public void addRootMenuItems() {

			add(getTitleItem());
			add(getNewRootFolderItem());
		}
		
		@Override
		protected JMenuItem getDetailsItem() {
	        JMenuItem detailsMenuItem = new JMenuItem("  View details");
	        detailsMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
			        new EntityDetailsDialog().showForEntityData(entityData);
				}
			});
	        return detailsMenuItem;
		}

		private JMenuItem getDeleteItem() {

			JMenuItem deleteItem = new JMenuItem("  Remove '"+entity.getName()+"'");
			deleteItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {

					final List<EntityData> eds = ModelMgr.getModelMgr().getParentEntityDatas(entity.getId());

					boolean removeReference = true;
					
					if (eds.size() <= 1 && entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
						// Make sure its not a reference to a common root
						if (entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)==null || eds.isEmpty()) {
							int deleteConfirmation = JOptionPane.showConfirmDialog(browser,
									"Are you sure you want to permanently delete '" + entity.getName()
											+ "' and all orphaned items underneath it?", "Delete",
									JOptionPane.YES_NO_OPTION);
							if (deleteConfirmation != 0) {
								return;
							}
							removeReference = false;
						}
						else if (entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null && entityData.getId()==null) {
							JOptionPane.showMessageDialog(browser, "Cannot delete this root entity because there are other references to it", "Error",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
					}

					Utils.setWaitingCursor(browser);

					final boolean removeRefFinal = removeReference;
					SimpleWorker removeTask = new SimpleWorker() {

						@Override
						protected void doStuff() throws Exception {
							// Update database
							if (removeRefFinal) {
								ModelMgr.getModelMgr().removeEntityData(entityData);
							} 
							else {
								ModelMgr.getModelMgr().deleteEntityTree(entity.getId());
							}
						}

						@Override
						protected void hadSuccess() {
							Utils.setDefaultCursor(browser);

							DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
							Entity parent = getEntity(parentNode);

							// Update object model
							parent.getEntityData().remove(entityData);

							// Update Tree UI
							selectedTree.removeNode(node);
							
							// Update the viewer
							selectNode(null);
						}

						@Override
						protected void hadError(Throwable error) {
							Utils.setDefaultCursor(browser);
							error.printStackTrace();
							JOptionPane.showMessageDialog(browser, "Error deleting entity", "Error",
									JOptionPane.ERROR_MESSAGE);
						}

					};

					removeTask.execute();
				}
			});

			if (!entityData.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
				deleteItem.setEnabled(false);
			}
			return deleteItem;
		}

		private JMenuItem getNewFolderItem() {

			if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER))
				return null;

			JMenuItem newFolderItem = new JMenuItem("  Create new folder");
			newFolderItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {

					// Add button clicked
					String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
							"Create folder under " + entity.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
					if ((folderName == null) || (folderName.length() <= 0)) {
						return;
					}

					try {
						// Update database
						Entity parentFolder = entity;
						Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
						EntityData newData = ModelMgr.getModelMgr().addEntityToParent(parentFolder, newFolder,
								parentFolder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);

						// Update these references to use our local objects, so that the object graph is consistent
						newData.setParentEntity(parentFolder);
						newData.setChildEntity(newFolder);

						// Update object model
						parentFolder.getEntityData().add(newData);

						// Update Tree UI
						addNodes(node, newData);

						selectedTree.expand(node, true);

					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(browser, "Error creating folder", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			});

			if (!entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
				newFolderItem.setEnabled(false);
			}
			return newFolderItem;
		}

		private JMenuItem getNewRootFolderItem() {

			JMenuItem newFolderItem = new JMenuItem("  Create new top-level folder");
			newFolderItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {

					// Add button clicked
					String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
							"Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
					if ((folderName == null) || (folderName.length() <= 0)) {
						return;
					}

					try {
						// Update database
						final Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
						newFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
						ModelMgr.getModelMgr().saveOrUpdateEntity(newFolder);

						// Update object model
						addTopLevelEntity(getRootEntity(), newFolder);

						// Update Tree UI
						refresh(true, new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								selectEntityByUniqueId("/e_"+newFolder.getId());
								return null;
							}
						});
						
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(browser, "Error creating folder", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			});

			return newFolderItem;
		}

		private JMenuItem getCreateSessionItem() {

			if (node.isRoot()) return null;
			
			JMenuItem newFragSessionItem = new JMenuItem("  Create Annotation Session for Neuron Fragments...");
			newFragSessionItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {

					DefaultMutableTreeNode node = selectedTree.getCurrentNode();
					final Entity entity = getEntity(node);

					try {
						Utils.setWaitingCursor(EntityOutline.this);

						SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, true) {

							protected void doneLoading() {
								Utils.setDefaultCursor(EntityOutline.this);
								List<Entity> entities = entity.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT, true);
								browser.getAnnotationSessionPropertyDialog().showForNewSession(entity.getName(), entities);
								SwingUtilities.updateComponentTreeUI(EntityOutline.this);
							}

							@Override
							protected void hadError(Throwable error) {
								error.printStackTrace();
								Utils.setDefaultCursor(EntityOutline.this);
								JOptionPane.showMessageDialog(browser, "Error loading nodes", "Internal Error",
										JOptionPane.ERROR_MESSAGE);
							}
						};

						loadingWorker.execute();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			return newFragSessionItem;
		}
	}

	/**
	 * Override this method to show a popup menu when the user right clicks a
	 * node in the tree.
	 * 
	 * @param e
	 */
	protected void showPopupMenu(MouseEvent e) {

		// Clicked on what node?
		final DefaultMutableTreeNode node = selectedTree.getCurrentNode();

		// Create context menu
		final EntityOutlineContextMenu popupMenu = new EntityOutlineContextMenu(node);
			
		if ("".equals(getRootEntity().getEntityType().getName())) return;
		
		if (node != null) {
			final Entity entity = getEntity(node);
			if (entity == null) return;
//			selectNode(node);
			popupMenu.addMenuItems();
		} 
		else {			
			popupMenu.addRootMenuItems();
		}

		popupMenu.show(selectedTree.getTree(), e.getX(), e.getY());	
	}

	/**
	 * Override this method to do something when the user left clicks a node.
	 * 
	 * @param e
	 */
	protected void nodeClicked(MouseEvent e) {
		selectNode(selectedTree.getCurrentNode());
	}

	/**
	 * Override this method to do something when the user presses down on a
	 * node.
	 * 
	 * @param e
	 */
	protected void nodePressed(MouseEvent e) {
	}

	/**
	 * Override this method to do something when the user double clicks a node.
	 * 
	 * @param e
	 */
	protected void nodeDoubleClicked(MouseEvent e) {
	}

	/**
	 * Reload the data for the current tree.
	 */
	@Override
	public void refresh() {
		refresh(true, null);
	}
	
	public void refresh(final boolean restoreState, final Callable<Void> success) {
		
		showLoadingIndicator();
		
		final ExpansionState expansionState = new ExpansionState();
		expansionState.storeExpansionState(getDynamicTree());

		SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

			private List<Entity> rootList;

			protected void doStuff() throws Exception {
				rootList = loadRootList();
			}

			protected void hadSuccess() {
				try {
					
					init(rootList);
					currUniqueId = null;
					
					if (restoreState) {
						expansionState.restoreExpansionState(getDynamicTree(), true);
					}
					
					showTree();
					
					if (success!=null) success.call();
				}
				catch (Exception e) {
					hadError(e);
				}
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(EntityOutline.this, "Error loading data outline", "Data Load Error",
						JOptionPane.ERROR_MESSAGE);
				init(null);
			}
		};

		entityOutlineLoadingWorker.execute();
	}
	
	public void expandByUniqueId(String uniqueId) {
		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		getDynamicTree().expand(node, true);
	}

	public void selectEntityByUniqueId(String uniqueId) {
		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		selectNode(node);
	}
    
	private synchronized void selectNode(final DefaultMutableTreeNode node) {

		final IconDemoPanel panel = ((IconDemoPanel)SessionMgr.getBrowser().getActiveViewer());
		
		if (node == null) {
			currUniqueId = null;
			panel.clear();
			return;
		}

		String uniqueId = getDynamicTree().getUniqueId(node);
		if (uniqueId.equals(currUniqueId)) return;
		this.currUniqueId = uniqueId;
		
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
		if (parentNode != null && !getTree().isExpanded(new TreePath(parentNode.getPath()))) {
			getDynamicTree().expand(parentNode, true);
		}

		getDynamicTree().navigateToNode(node);

		panel.showLoadingIndicator();

		final String finalCurrUniqueId = currUniqueId;
		
		if (!getDynamicTree().childrenAreLoaded(node)) {
			// Load the children before displaying them
			SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, false) {
				@Override
				protected void doneLoading() {
					loadEntityInViewer(finalCurrUniqueId);
				}
			};
			loadingWorker.execute();
		} 
		else {
			loadEntityInViewer(finalCurrUniqueId);
		}

	}
	
	private void loadEntityInViewer(String uniqueId) {
		// Make sure this load is still relevant. User could have selected a new node in the meantime. 
		if (uniqueId==null || !uniqueId.equals(currUniqueId)) return;
		
		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		if (node==null) return;
		
		// This method would never be called on a node whose children are lazy
		if (!selectedTree.childrenAreLoaded(node)) {
			throw new IllegalStateException("Cannot display entity whose children are not loaded");
		}
		
		((IconDemoPanel)SessionMgr.getSessionMgr().getActiveBrowser().getActiveViewer()).loadEntity(getEntity(node));
		ModelMgr.getModelMgr().selectOutlineEntity(uniqueId, true);
	}
}
