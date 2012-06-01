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
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.shared.utils.EntityUtils;

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
			public void entitySelected(String category, String entityId, boolean clearAll) {
				if (EntitySelectionModel.CATEGORY_OUTLINE.equals(category)) {
					selectEntityByUniqueId(entityId);
				}
			}

			@Override
			public void entityDeselected(String category, String entityId) {
				if (EntitySelectionModel.CATEGORY_OUTLINE.equals(category)) {
					getTree().clearSelection();
				}
			}

			@Override
			public void entityChanged(final long entityId) {
				refresh();
			}

			@Override
			public void entityRemoved(long entityId) {
				Set<DefaultMutableTreeNode> nodes = getNodesByEntityId(entityId);
				if (nodes == null) return;
				for(DefaultMutableTreeNode node : new HashSet<DefaultMutableTreeNode>(nodes)) {
					DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
					Entity parent = getEntity(parentNode);
					EntityData entityData = getEntityData(node);
					if (parent!=null) {
						parent.getEntityData().remove(entityData);
					}
					removeNode(node);	
				}
			}

			@Override
			public void entityDataRemoved(long entityDataId) {
				Set<DefaultMutableTreeNode> nodes = getNodesByEntityDataId(entityDataId);
				if (nodes == null) return;
				for(DefaultMutableTreeNode node : new HashSet<DefaultMutableTreeNode>(nodes)) {
					DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
					Entity parent = getEntity(parentNode);
					EntityData entityData = getEntityData(node);
					if (parent!=null) {
						parent.getEntityData().remove(entityData);
					}
					removeNode(node);	
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

		public EntityOutlineContextMenu(DefaultMutableTreeNode node, String uniqueId) {
			super(new RootedEntity(uniqueId, getEntityData(node)));
		}
		
		public void addRootMenuItems() {
			add(getRootItem());
			add(getNewRootFolderItem());
		}

		protected JMenuItem getRootItem() {
	        JMenuItem titleMenuItem = new JMenuItem("Data");
	        titleMenuItem.setEnabled(false);
	        return titleMenuItem;
		}

		private JMenuItem getNewRootFolderItem() {
			if (multiple) return null;
			
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
						Entity newFolder = ModelMgrUtils.createNewCommonRoot(folderName);

						// Update Tree UI
						final Long newFolderId = newFolder.getId();
						refresh(true, new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								selectEntityByUniqueId("/e_"+newFolderId);
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
		final EntityOutlineContextMenu popupMenu = new EntityOutlineContextMenu(node, selectedTree.getUniqueId(node));
			
		if ("".equals(getRootEntity().getEntityType().getName())) return;
		
		if (node != null) {
			final Entity entity = getEntity(node);
			if (entity == null) return;
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
		final ExpansionState expansionState = new ExpansionState();
		expansionState.storeExpansionState(getDynamicTree());
		refresh(restoreState, expansionState, success);
	}
	
	public void refresh(final boolean restoreState, final ExpansionState expansionState, final Callable<Void> success) {
		
		showLoadingIndicator();
		
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
						expansionState.restoreExpansionState(getDynamicTree(), true, new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								showTree();
								if (success!=null) success.call();
								return null;
							}
						});
					}
					
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
	
	public void expandByUniqueId(final String uniqueId) {
		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		if (node!=null) {
			getDynamicTree().expand(node, true);
			return;
		}

		// Let's try to lazy load the ancestors of this node
		List<String> path = EntityUtils.getPathFromUniqueId(uniqueId);
		for (String ancestorId : path) {
			DefaultMutableTreeNode ancestor = getNodeByUniqueId(ancestorId);
			if (ancestor==null) {
				// Give up, can't find the entity with this uniqueId
				System.out.println("EntityOutline.expandByUniqueId: cannot locate "+uniqueId);
				return;
			}
			if (!getDynamicTree().childrenAreLoaded(ancestor)) {
				// Load the children before displaying them
				SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, ancestor, false) {
					@Override
					protected void doneLoading() {
						expandByUniqueId(uniqueId);
					}
				};
				loadingWorker.execute();
				return;
			}
		}
	}
	
	public void selectEntityByUniqueId(final String uniqueId) {
		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		if (node!=null) {
			selectNode(node);	
			return;
		}
		
		// Let's try to lazy load the ancestors of this node
		List<String> path = EntityUtils.getPathFromUniqueId(uniqueId);
		for (String ancestorId : path) {
			DefaultMutableTreeNode ancestor = getNodeByUniqueId(ancestorId);
			if (ancestor==null) {
				// Give up, can't find the entity with this uniqueId
				System.out.println("EntityOutline.selectEntityByUniqueId: cannot locate "+uniqueId);
				return;
			}
			if (!getDynamicTree().childrenAreLoaded(ancestor)) {
				// Load the children before displaying them
				SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, ancestor, false) {
					@Override
					protected void doneLoading() {
						selectEntityByUniqueId(uniqueId);
					}
				};
				loadingWorker.execute();
				return;
			}
		}
	}
    
	private synchronized void selectNode(final DefaultMutableTreeNode node) {

		final IconDemoPanel panel = ((IconDemoPanel)SessionMgr.getBrowser().getActiveViewer());
		
		if (node == null) {
			currUniqueId = null;
			return;
		}

		String uniqueId = getDynamicTree().getUniqueId(node);
		if (!uniqueId.equals(currUniqueId)) {
			this.currUniqueId = uniqueId;
			ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId+"", true);
		}
		
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
		
		if (uniqueId==null) return;
		
		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		if (node==null) return;
		
		// This method would never be called on a node whose children are lazy
		if (!selectedTree.childrenAreLoaded(node)) {
			throw new IllegalStateException("Cannot display entity whose children are not loaded");
		}
		
		RootedEntity rootedEntity = new RootedEntity(uniqueId, getEntityData(node));
		((IconDemoPanel)SessionMgr.getSessionMgr().getActiveBrowser().getActiveViewer()).loadEntity(rootedEntity);
	}
}
