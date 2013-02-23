package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.utils.ModelUtils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * The entity wrapper tree which lives in the right-hand "Data" panel and drives the viewers. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityWrapperOutline extends EntityWrapperTree implements Cloneable, Refreshable, ActivatableView {
	
	private static final Logger log = LoggerFactory.getLogger(EntityWrapperOutline.class);

    private ModelMgrAdapter mml;
	private String currUniqueId;
    private AlignmentContext alignmentContext;

    public EntityWrapperOutline() {
		super(true);
		this.setMinimumSize(new Dimension(400, 400));
		showLoadingIndicator();

        this.mml = new ModelMgrAdapter() {
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
        };
		
		// TODO: these parameters should be picked from a list, by the user, when creating the alignment board
        AlignmentContext alignmentContext = new AlignmentContext(
                "Unified 20x Alignment Space", "0.62x0.62x0.62", "1024x512x218");
        setAlignmentContext(alignmentContext);
	}

    public AlignmentContext getAlignmentContext() {
        return alignmentContext;
    }

    public void setAlignmentContext(AlignmentContext alignmentContext) {
        this.alignmentContext = alignmentContext;
    }
    
    @Override
    public void activate() {
        log.info("Activating");
        super.activate();
        ModelMgr.getModelMgr().addModelMgrObserver(mml);
        if (getRoot()==null) {
            refresh();
        }
    }

    @Override
    public void deactivate() {
        log.info("Deactivating");
        super.deactivate();
        ModelMgr.getModelMgr().removeModelMgrObserver(mml);
    }

    @Override
	public void initializeTree(List<EntityWrapper> roots) {
	    
		super.initializeTree(roots);
				
		getDynamicTree().expand(getDynamicTree().getRootNode(), true);
		
		JTree tree = getTree();
		tree.setRootVisible(false);
		tree.setDragEnabled(true);
//		tree.setDropMode(DropMode.ON_OR_INSERT);
		tree.setTransferHandler(new EntityWrapperTransferHandler() {
			@Override
			public JComponent getDropTargetComponent() {
				return EntityWrapperOutline.this;
			}
		});

		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true),"enterAction");
		getActionMap().put("enterAction",new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectEntityByUniqueId(getCurrUniqueId());
			}
		});
	}
	
	/**
	 * Override this method to load the root list. This method will be called in
	 * a worker thread.
	 * 
	 * @return
	 */
	public abstract List<EntityWrapper> loadRootList() throws Exception;

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
			
			JMenuItem newFolderItem = new JMenuItem("  Create New Top-Level Folder");
			newFolderItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {

					// Add button clicked
					final String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n",
							"Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
					if ((folderName == null) || (folderName.length() <= 0)) {
						return;
					}

					SimpleWorker worker = new SimpleWorker() {
						private Entity newFolder;
						@Override
						protected void doStuff() throws Exception {
							// Update database
							newFolder = ModelMgr.getModelMgr().createCommonRoot(folderName);
						}
						@Override
						protected void hadSuccess() {
							// Update Tree UI
							totalRefresh(true, new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									selectEntityByUniqueId("/e_"+newFolder.getId());
									return null;
								}
							});
						}
						@Override
						protected void hadError(Throwable error) {
							SessionMgr.getSessionMgr().handleException(error);
						}
					};
					worker.execute();
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
	protected void showPopupMenu(final MouseEvent e) {

		// Clicked on what node?
		final DefaultMutableTreeNode node = getDynamicTree().getCurrentNode();

		// Create context menu
		final EntityOutlineContextMenu popupMenu = new EntityOutlineContextMenu(node, getDynamicTree().getUniqueId(node));
			
		if ("".equals(getRoot().getType())) return;
		
		if (node != null) {
			final Entity entity = getEntity(node);
			if (entity == null) return;
			popupMenu.addMenuItems();
		} 
		else {			
			popupMenu.addRootMenuItems();
		}

		popupMenu.show(getDynamicTree().getTree(), e.getX(), e.getY());
	}

	/**
	 * Override this method to do something when the user left clicks a node.
	 * 
	 * @param e
	 */
    @Override
	protected void nodeClicked(MouseEvent e) {
		this.currUniqueId = null;
		selectNode(getDynamicTree().getCurrentNode());
	}

	/**
	 * Override this method to do something when the user presses down on a
	 * node.
	 * 
	 * @param e
	 */
    @Override
	protected void nodePressed(MouseEvent e) {
	}

	/**
	 * Override this method to do something when the user double clicks a node.
	 * 
	 * @param e
	 */
    @Override
	protected void nodeDoubleClicked(MouseEvent e) {
	}

	@Override
    protected void loadLazyNode(DefaultMutableTreeNode node) throws Exception {
        EntityWrapper wrapper = getEntityWrapper(node);
        wrapper.loadContextualizedChildren(getAlignmentContext());
    }

    
    @Subscribe 
    public void entityInvalidated(EntityInvalidationEvent event) {
        log.info("Some entities were invalidated so we're refreshing the outline");
        refresh(false, true, null);
    }

	@Override
	public void refresh() {
		refresh(true, null);
	}

	@Override
	public void totalRefresh() {
		totalRefresh(true, null);
	}

	public void refresh(final boolean restoreState, final Callable<Void> success) {
	    refresh(false, restoreState, success);
	}
	
	public void totalRefresh(final boolean restoreState, final Callable<Void> success) {
	    refresh(true, restoreState, success);
	}
	
    public void refresh(final boolean invalidateCache, final boolean restoreState, final Callable<Void> success) {
        if (restoreState) {
            final ExpansionState expansionState = new ExpansionState();
            expansionState.storeExpansionState(getDynamicTree());
            refresh(invalidateCache, restoreState, expansionState, success);
        }
        else {
            refresh(invalidateCache, false, null, success);
        }
    }
    
	private AtomicBoolean refreshInProgress = new AtomicBoolean(false);
	
	public void refresh(final boolean invalidateCache, final boolean restoreState, final ExpansionState expansionState, final Callable<Void> success) {
		
		if (refreshInProgress.getAndSet(true)) {
			log.debug("Skipping refresh, since there is one already in progress");
			return;
		}
		
		log.debug("Starting whole tree refresh (invalidateCache={}, restoreState={})",invalidateCache,restoreState);
		
		showLoadingIndicator();
		
		SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

			private List<EntityWrapper> rootList;

			protected void doStuff() throws Exception {
				if (invalidateCache) {
					ModelMgr.getModelMgr().invalidateCache(ModelUtils.getInternalEntities(getRoot().getChildren()), true);
				}
				rootList = loadRootList();
			}

			protected void hadSuccess() {
				try {
				    initializeTree(rootList);
					currUniqueId = null;
					refreshInProgress.set(false);
					
					if (restoreState) {
						expansionState.restoreExpansionState(getDynamicTree(), true, new Callable<Void>() {
							@Override
							public Void call() throws Exception {
								showTree();
								if (success!=null) success.call();
								log.debug("Tree refresh complete");
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
				refreshInProgress.set(false);
				log.error("Tree refresh encountered error",error);
				JOptionPane.showMessageDialog(EntityWrapperOutline.this, "Error loading data outline", "Data Load Error",
						JOptionPane.ERROR_MESSAGE);
				initializeTree(null);
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
				log.warn("expandByUniqueId cannot locate "+uniqueId);
				return;
			}
			if (!getDynamicTree().childrenAreLoaded(ancestor)) {
				// Load the children before displaying them
				getDynamicTree().expandNodeWithLazyChildren(ancestor, new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						expandByUniqueId(uniqueId);
						return null;
					}
					
				});
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
				log.warn("selectEntityByUniqueId cannot locate "+uniqueId);
				return;
			}
			if (!getDynamicTree().childrenAreLoaded(ancestor)) {
				// Load the children before displaying them
				getDynamicTree().expandNodeWithLazyChildren(ancestor, new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						selectEntityByUniqueId(uniqueId);
						return null;
					}
					
				});
				return;
			}
		}
	}
    
	private synchronized void selectNode(final DefaultMutableTreeNode node) {

		// TODO: this should be encapsulated away from here somehow
		ScreenEvaluationDialog screenEvaluationDialog = SessionMgr.getBrowser().getScreenEvaluationDialog();
		if (screenEvaluationDialog.isCurrFolderDirty()) {
			screenEvaluationDialog.setCurrFolderDirty(false);
			if (screenEvaluationDialog.isAutoMoveAfterNavigation()) {
				screenEvaluationDialog.organizeEntitiesInCurrentFolder(true, new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						selectNode(node);
						return null;
					}
				});
				return;
			}
			else if (screenEvaluationDialog.isAskAfterNavigation()) {
				Object[] options = {"Yes", "No", "Organize now"};
				int c = JOptionPane.showOptionDialog(SessionMgr.getBrowser(),
						"Are you sure you want to navigate away from this folder without organizing it?", "Navigate",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[2]);
				if (c == 1) {
					return;
				}
				else if (c == 2) {
					screenEvaluationDialog.organizeEntitiesInCurrentFolder(true, new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							selectNode(node);
							return null;
						}
					});
					return;
				}
			}
		}
		
		if (node == null) {
			currUniqueId = null;
			return;
		}
		
		String uniqueId = getDynamicTree().getUniqueId(node);
		if (!uniqueId.equals(currUniqueId)) {
			this.currUniqueId = uniqueId;
			ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId+"", true);
		}
		else {
			return;
		}

		log.debug("Selecting node {}",uniqueId);
		
		DefaultMutableTreeNode node2 = getNodeByUniqueId(uniqueId);
		
		if (node2==null) {
			log.warn("selectNode cannot locate "+uniqueId);
			return;
		}
		
		if (node!=node2) {
			log.error("We have a node conflict. This should never happen!");
		}
		
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
		if (parentNode != null && !getTree().isExpanded(new TreePath(parentNode.getPath()))) {
			getDynamicTree().expand(parentNode, true);
		}

		getDynamicTree().navigateToNode(node);

		final String finalCurrUniqueId = currUniqueId;
		
		// TODO: should decouple this somehow
		
//		if (!getDynamicTree().childrenAreLoaded(node)) {
//			SessionMgr.getBrowser().getViewerManager().getActiveViewer().showLoadingIndicator();
//			// Load the children before displaying them
//        	getDynamicTree().expandNodeWithLazyChildren(node, new Callable<Void>() {
//				@Override
//				public Void call() throws Exception {
//				    log.debug("Got lazy nodes, loading entity in viewer");
//					loadEntityInViewer(finalCurrUniqueId);
//					return null;
//				}
//				
//			});
//		} 
//		else {
//			loadEntityInViewer(finalCurrUniqueId);
//		}
	}
	
	private void loadEntityInViewer(String uniqueId) {
		
//	    log.debug("loadEntityInViewer: "+uniqueId);
//		if (uniqueId==null) return;
//
//		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
//		if (node==null) return;
//		
//		EntityWrapper wrapper = getEntityWrapper(node);
//		// TODO: should use the wrapper.getChildren() at some point here, instead of relying on the entity model
//		RootedEntity rootedEntity = new RootedEntity(uniqueId, getEntityData(node));
//		
//		log.debug("showEntityInActiveViewer: "+uniqueId);
//		SessionMgr.getBrowser().getViewerManager().showEntityInActiveViewer(rootedEntity);
	}
}
