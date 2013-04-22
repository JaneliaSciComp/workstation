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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityCreateEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.FlyWorkstation.gui.framework.console.Perspective;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * The entity tree which lives in the right-hand "Data" panel and drives the viewers. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityOutline extends EntityTree implements Refreshable, ActivatableView {
	
	private static final Logger log = LoggerFactory.getLogger(EntityOutline.class);
	
	private Entity root;
	private List<Entity> entityRootList;
	private ModelMgrAdapter mml;
	private String currUniqueId;

    public EntityOutline() {
		super();
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
	}
    
	@Override
    public void activate() {
	    log.info("Activating");
        super.activate();
        ModelMgr.getModelMgr().addModelMgrObserver(mml);
        refresh();
    }

    @Override
    public void deactivate() {
        log.info("Deactivating");
        super.deactivate();
        ModelMgr.getModelMgr().removeModelMgrObserver(mml);
    }

    public void init(List<Entity> entityRootList) {
		
        this.entityRootList = entityRootList;
        
		EntityType rootType = new EntityType();
		rootType.setName("");
		rootType.setAttributes(new HashSet<EntityAttribute>());
		
		this.root = new Entity();
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
		ed.setOwnerKey(entity.getOwnerKey());
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
	public abstract List<Entity> loadRootList() throws Exception;

    private class EntityOutlineContextMenu extends EntityContextMenu {

		public EntityOutlineContextMenu(DefaultMutableTreeNode node, String uniqueId) {
			super(new RootedEntity(uniqueId, getEntityData(node)));
		}

        public void addRootMenuItems() {
            add(getRootItem());
            add(getNewRootFolderItem());
            add(getNewAlignmentBoardItem());
            add(getOpenAlignmentBoardItem());
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
						    SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    selectEntityByUniqueId("/e_"+newFolder.getId());
                                }
                            });
						    
							// Update Tree UI
//							totalRefresh(true, new Callable<Void>() {
//								@Override
//								public Void call() throws Exception {
//									selectEntityByUniqueId("/e_"+newFolder.getId());
//									return null;
//								}
//							});
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

        public JMenuItem getNewAlignmentBoardItem() {
            if (multiple) return null;

            JMenuItem newFolderItem = new JMenuItem("  Create New Alignment Board");
            newFolderItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {

                    // Add button clicked
                    final String boardName = (String) JOptionPane.showInputDialog(browser, "Board Name:\n",
                            "Create Alignment Board", JOptionPane.PLAIN_MESSAGE, null, null, null);
                    if ((boardName == null) || (boardName.length() <= 0)) {
                        return;
                    }

                    SimpleWorker worker = new SimpleWorker() {
                        private RootedEntity newBoard;
                        @Override
                        protected void doStuff() throws Exception {
                            // Update database
                            // TODO: this should ask the user what kind of alignment board they want to create
                            newBoard = ModelMgr.getModelMgr().createAlignmentBoard(boardName, "Unified 20x Alignment Space", "0.62x0.62x0.62", "1024x512x218");
                        }
                        @Override
                        protected void hadSuccess() {
                            // Update Tree UI
                            totalRefresh(true, new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    selectEntityByUniqueId(newBoard.getUniqueId());
                                    SessionMgr.getBrowser().setPerspective(Perspective.AlignmentBoard);
                                    SessionMgr.getBrowser().getLayersPanel().openAlignmentBoard(newBoard.getEntityId());
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
		this.currUniqueId = null;
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

    @Subscribe 
    public void entityCreated(EntityCreateEvent event) {
        Entity entity = event.getEntity();
        if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
            log.debug("New common root detected: '{}'",entity.getName());     
            
            
            int index = 0;
            for(EntityData ed : root.getOrderedEntityData()) {
                if (!ModelMgrUtils.isOwner(ed.getChildEntity())) {
                    break;
                }
                index++;
            }
            
            log.trace("Will insert at "+index);
            entityRootList.add(index, entity);
            
            int i = 0;
            for(EntityData ed : root.getOrderedEntityData()) {
                log.trace("i="+i+" orderIndex="+ed.getOrderIndex()+" "+ed.getChildEntity().getName());
                if (i>=index) {
                    log.trace("  Incrementing to "+(i+1));
                    ed.setOrderIndex(i+1);
                }
                i++;
            }
            
            EntityData newEd = addTopLevelEntity(root, entity);
            newEd.setOrderIndex(index);
            
            addNodes(getDynamicTree().getRootNode(), newEd, index);
        }
    }
    
	@Subscribe 
	public void entityInvalidated(EntityInvalidationEvent event) {
		log.debug("Some entities were invalidated so we're refreshing the tree");
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
            refresh(invalidateCache, expansionState, success);
        }
        else {
            refresh(invalidateCache, null, success);
        }
    }
	
	private AtomicBoolean refreshInProgress = new AtomicBoolean(false);
	
	public void refresh(final boolean invalidateCache, final ExpansionState expansionState, final Callable<Void> success) {
		
		if (refreshInProgress.getAndSet(true)) {
			log.debug("Skipping refresh, since there is one already in progress");
			return;
		}
		
		log.debug("Starting whole tree refresh (invalidateCache={}, restoreState={})",invalidateCache,expansionState!=null);
		
		showLoadingIndicator();
		
		SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

			private List<Entity> rootList;

			protected void doStuff() throws Exception {
				if (invalidateCache) {
					ModelMgr.getModelMgr().invalidateCache(getRootEntity().getChildren(), true);
				}
				rootList = loadRootList();
			}

			protected void hadSuccess() {
				try {
					init(rootList);
					currUniqueId = null;
					refreshInProgress.set(false);
					
					if (expansionState!=null) {
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
		
		if (!getDynamicTree().childrenAreLoaded(node)) {
			SessionMgr.getBrowser().getViewerManager().getActiveViewer().showLoadingIndicator();
			// Load the children before displaying them
        	getDynamicTree().expandNodeWithLazyChildren(node, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
				    log.debug("Got lazy nodes, loading entity in viewer");
					loadEntityInViewer(finalCurrUniqueId);
					return null;
				}
				
			});
		} 
		else {
			loadEntityInViewer(finalCurrUniqueId);
		}
	}
	
	private void loadEntityInViewer(String uniqueId) {
		
	    log.debug("loadEntityInViewer: "+uniqueId);
		if (uniqueId==null) return;

		DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
		if (node==null) return;
		
		// This method would never be called on a node whose children are lazy
		if (!getDynamicTree().childrenAreLoaded(node)) {
			throw new IllegalStateException("Cannot display entity whose children are not loaded");
		}
		
		RootedEntity rootedEntity = new RootedEntity(uniqueId, getEntityData(node));
		log.debug("showEntityInActiveViewer: "+rootedEntity.getName());
		SessionMgr.getBrowser().getViewerManager().showEntityInActiveViewer(rootedEntity);
	}
}
