package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.ExpansionState;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.framework.tree.TreeTransferHandler;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 * This class is the initial outline of the data file tree
 */
public abstract class EntityOutline extends EntityTree implements Cloneable, Outline {
    
    private List<Entity> entityRootList;
    private Entity selectedEntity;
    
    public EntityOutline() {
    	super(true);
        this.setMinimumSize(new Dimension(400, 400));
        showLoadingIndicator();

        ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {
			@Override
			public void entitySelected(final long entityId, final boolean outline, final boolean clearAll) {
				if (!outline) return;
				selectEntityById(entityId);
			}
        });
    }
    
    public void init(List<Entity> entityRootList) {
    	this.entityRootList = entityRootList;
        if (null != entityRootList && entityRootList.size() >= 1) {
            initializeTree(entityRootList.get(0).getId(), null);
        }
        else {
        	Entity noDataEntity = new Entity();
        	EntityType type = new EntityType();
        	type.setName("");
        	noDataEntity.setEntityType(type);
        	noDataEntity.setName("No data");
        	initializeTree(noDataEntity);
        } 
    }
    
    @Override
	public void initializeTree(Entity rootEntity) {
		super.initializeTree(rootEntity);

        JTree tree = getTree();
        tree.setDragEnabled(true);  
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler(getDynamicTree()) {

        	protected boolean allowTransfer(DefaultMutableTreeNode node, DefaultMutableTreeNode destination) {
        		Entity entity = getEntity(node);
        		Entity destEntity = getEntity(destination);
        		return (ModelMgrUtils.isOwner(entity) && ModelMgrUtils.isOwner(destEntity));
        	}
        	
        	protected boolean updateUserData(DefaultMutableTreeNode nodeRemoved, DefaultMutableTreeNode nodeAdded, DefaultMutableTreeNode newParent, int destIndex) {
        		
        		try {
            		if (nodeRemoved!=null) {
            			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)nodeRemoved.getParent();
            			Entity entity = getEntity(nodeRemoved);
            			Entity parent = getEntity(parentNode);
                		ModelMgr.getModelMgr().removeEntityFromParent(parent, entity);
            		}

            		if (nodeAdded!=null) {
	        			DefaultMutableTreeNode parentNode = newParent;
	        			Entity entity = getEntity(nodeAdded);
	        			Entity parent = getEntity(parentNode);
	            		
	            		// Add to parent
	            		EntityData newEd = parent.addChildEntity(entity);
	            		// Temporarily remove it so that it can be inserted with the correct index
	            		parent.getEntityData().remove(newEd); 
	            		
	            		List<EntityData> eds = EntityUtils.getOrderedEntityDataOfType(parent, EntityConstants.ATTRIBUTE_ENTITY);
	            		if (destIndex>eds.size()) {
	            			eds.add(newEd);
	            		}
	            		else {
	            			eds.add(destIndex, newEd);	
	            		}
	            		
	            		// Renumber the children
	            		int index = 0;
	            		for(EntityData ed : eds) {
	            			if (ed.getOrderIndex()==null || ed.getOrderIndex()!=index) {
	            				ed.setOrderIndex(index);
	            				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
	                    		if (index==destIndex) {
	                        		// Re-add the saved entity data to the parent
	                        		parent.getEntityData().add(savedEd);
	                    		}
	            			}
	            			index++;
	            		}
            		}
            		
            		return true;
        		}
        		catch (Exception e) {
        			SessionMgr.getSessionMgr().handleException(e);
        			return false;
        		}
        	}
        	
        	protected boolean addNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode node, int index) {
        		addNodes(parent, (Entity)node.getUserObject(), index);
        		return true;
        	}
        	
        }); 
	}

	/**
     * Override this method to load the root list. This method will be called in a worker thread.
     * @return
     */
    public abstract List<Entity> loadRootList();
    
    private class EntityOutlineContextMenu extends EntityContextMenu {

    	private DefaultMutableTreeNode node;
		public EntityOutlineContextMenu(DefaultMutableTreeNode node) {
			super((Entity)node.getUserObject());
			this.node = node;
		}

		public void addMenuItems() {
            
    		add(getTitleItem());
            add(getCopyToClipboardItem());
            add(getDetailsItem());
            add(getRenameItem());
            add(getDeleteItem());
            add(getNewFolderItem());
            
            setNextAddRequiresSeparator(true);
        	add(getOpenInFinderItem());
        	add(getOpenWithAppItem());
            add(getNeuronAnnotatorItem());
            
            setNextAddRequiresSeparator(true);
            add(getChangeDataRootItem());
            add(getCreateSessionItem());
    	}

    	private JMenuItem getDeleteItem() {
                
            JMenuItem deleteItem = new JMenuItem("  Delete tree");
            deleteItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {

                    int deleteConfirmation = JOptionPane.showConfirmDialog(browser, "Are you sure you want to permanently delete this item and all items under it?", "Delete", JOptionPane.YES_NO_OPTION);
                    if (deleteConfirmation != 0) {
                        return;
                    }

    	            Utils.setWaitingCursor(browser);
    	            
    	            SimpleWorker removeTask = new SimpleWorker() {

    	                @Override
    	                protected void doStuff() throws Exception {
    	    	            // Update database
    	                    ModelMgr.getModelMgr().deleteEntityTree(entity.getId());
    	                }

    	                @Override
    	                protected void hadSuccess() {
    	                	Utils.setDefaultCursor(browser);

    	                	DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)node.getParent();
    	                	Entity parent = (Entity)parentNode.getUserObject();

    	                    // Update object model
    	                	EntityUtils.removeChild(parent, entity);
    	                	    	                    
    	                    // Update Tree UI
    	                    selectedTree.removeNode(node);
    	                }

    	                @Override
    	                protected void hadError(Throwable error) {
    	                	Utils.setDefaultCursor(browser);
                            error.printStackTrace();
                            JOptionPane.showMessageDialog(browser, "Error deletings entity", "Error", JOptionPane.ERROR_MESSAGE);
    	                }

    	            };

    	            removeTask.execute();
                }
            });

            if (!entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
            	deleteItem.setEnabled(false);
            }
            return deleteItem;
    	}
    	
    	private JMenuItem getNewFolderItem() {
            
            if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) return null;
                
            JMenuItem newFolderItem = new JMenuItem("  Create new folder");
            newFolderItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {

                    // Add button clicked
                    String folderName = (String) JOptionPane.showInputDialog(browser, "Folder Name:\n", "Create folder under "+entity.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
                    if ((folderName == null) || (folderName.length() <= 0)) {
                        return;
                    }

                    try {
                        // Update database
                    	Entity parentFolder = entity;
                        Entity newFolder = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_FOLDER, folderName);
                        EntityData newData = ModelMgr.getModelMgr().addEntityToParent(parentFolder, newFolder, parentFolder.getMaxOrderIndex(), EntityConstants.ATTRIBUTE_ENTITY);
                        
                        // Update these references to use our local objects, so that the object graph is consistent
                        newData.setParentEntity(parentFolder);
                        newData.setChildEntity(newFolder);
                        
                        // Update object model
                        parentFolder.getEntityData().add(newData);

                        // Update Tree UI
                        addNodes(node, newFolder);

                        selectedTree.expand(node, true);

                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(browser, "Error creating folder", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            if (!entity.getUser().getUserLogin().equals(SessionMgr.getUsername())) {
            	newFolderItem.setEnabled(false);
            }
            return newFolderItem;
    	}
    	
    	private JMenuItem getChangeDataRootItem() {
    		if (!node.isRoot()) return null;
            final JMenu changeDataSourceMenu = new JMenu("  Change data root...");

            changeDataSourceMenu.addMenuListener(new MenuListener() {
    			
    			@Override
    			public void menuSelected(MenuEvent e) {

    				changeDataSourceMenu.removeAll();
    				JMenuItem loadingItem = new JMenuItem("Loading...");
    				loadingItem.setEnabled(false);
	                changeDataSourceMenu.add(loadingItem);
	                
    				SimpleWorker menuWorker = new SimpleWorker() {
						
						@Override
						protected void doStuff() throws Exception {
							entityRootList = loadRootList();
						}
						
						@Override
						protected void hadSuccess() {
							changeDataSourceMenu.removeAll();
				        	for(final Entity commonRoot : entityRootList) {
				                final JMenuItem dataSourceItem = new JCheckBoxMenuItem(
				                		commonRoot.getName() +" ("+commonRoot.getUser().getUserLogin()+")", 
				                		commonRoot.getId().equals(selectedEntity.getId()));
				                dataSourceItem.addActionListener(new ActionListener() {
				                    public void actionPerformed(ActionEvent actionEvent) {
				                    	initializeTree(commonRoot.getId(), null);
				                    }
				                });
				                changeDataSourceMenu.add(dataSourceItem);
				        	}

				        	// A little hack to refresh the submenu. Just calling revalidate/repaint will show the new
				        	// contents but not resize the menu to fit. 
				        	if (changeDataSourceMenu.isSelected()) {
					        	changeDataSourceMenu.setPopupMenuVisible(false);
				        		changeDataSourceMenu.setPopupMenuVisible(true);
				        	}
						}
						
						@Override
						protected void hadError(Throwable error) {
							error.printStackTrace();
						}
					};
					
					menuWorker.execute();
    			}
    			
    			@Override
    			public void menuDeselected(MenuEvent e) {
    			}
    			
    			@Override
    			public void menuCanceled(MenuEvent e) {
    			}
    		});
            
    		return changeDataSourceMenu;
    	}
    	
    	private JMenuItem getCreateSessionItem() {

    		if (node.isRoot()) return null;
	        JMenuItem newFragSessionItem = new JMenuItem("  Create Annotation Session for Neuron Fragments...");
	        newFragSessionItem.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent actionEvent) {
	
	                DefaultMutableTreeNode node = selectedTree.getCurrentNode();
	                final Entity entity = (Entity) node.getUserObject();
	
	                try {
	                    Utils.setWaitingCursor(EntityOutline.this);
	
	                    SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, true) {
	
	                        protected void doneLoading() {
	                            Utils.setDefaultCursor(EntityOutline.this);
	                            List<Entity> entities = entity.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT);
	                            browser.getAnnotationSessionPropertyDialog().showForNewSession(entity.getName(), entities);
	                            SwingUtilities.updateComponentTreeUI(EntityOutline.this);
	                        }
	
	                        @Override
	                        protected void hadError(Throwable error) {
	                        	error.printStackTrace();
	                            Utils.setDefaultCursor(EntityOutline.this);
	                            JOptionPane.showMessageDialog(browser, "Error loading nodes", "Internal Error", JOptionPane.ERROR_MESSAGE);
	                        }
	                    };
	
	                    loadingWorker.execute();
	                }
	                catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        });
	        
	        return newFragSessionItem;
    	}
    }
    
    
    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {

    	// Clicked on what node?
        final DefaultMutableTreeNode node = selectedTree.getCurrentNode();
        if (node == null) return;
        final Entity entity = (Entity) node.getUserObject();
    	if (entity == null) return;

        selectNode(node);

        // Create context menus
        final EntityOutlineContextMenu popupMenu = new EntityOutlineContextMenu(node);
        popupMenu.addMenuItems();
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
     * Override this method to do something when the user presses down on a node.
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
    	if (entityRootList == null || entityRootList.isEmpty()) {
            SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

                private List<Entity> rootList;
            	
                protected void doStuff() throws Exception {
                	rootList = loadRootList();
                }

                protected void hadSuccess() {
                	init(rootList);
                }

                protected void hadError(Throwable error) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(EntityOutline.this, 
                    		"Error loading data outline", "Data Load Error", JOptionPane.ERROR_MESSAGE);
                    init(null);
                }
            };
            
            entityOutlineLoadingWorker.execute();
    	}
    	else if (getRootEntity()!=null) {
	        Utils.setWaitingCursor(EntityOutline.this);
	    	final ExpansionState expansionState = new ExpansionState();
	    	expansionState.storeExpansionState(getDynamicTree());
	    	initializeTree(getRootEntity().getId(), new Callable<Void>() {
				@Override
				public Void call() throws Exception {
			    	expansionState.restoreExpansionState(getDynamicTree());
	                Utils.setDefaultCursor(EntityOutline.this);
					return null;
				}
			});
    	}
    }
    
    private void selectNode(DefaultMutableTreeNode node) {
    	selectEntity((Entity)node.getUserObject());
    }
    
    private void selectEntity(Entity entity) {
    	selectEntityById(entity.getId());
    }
    
    private void selectEntityById(long entityId) {
    	
    	DefaultMutableTreeNode node = getNodeByEntityId(entityId);
    	if (node==null) return;
    	
    	Entity entity = (Entity)node.getUserObject();
    	if (entity==null) return;
    	if (Utils.areSame(entity, selectedEntity)) return;
    	
    	DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
    	if (parent!=null && !getTree().isExpanded(new TreePath(parent.getPath()))) {
    		getDynamicTree().expand(parent, true);
    	}
    	
    	getDynamicTree().navigateToNode(node);
    	
    	selectedEntity = entity;
    	
        ModelMgr.getModelMgr().selectEntity(entity.getId(), true, true);
    	
    	revalidate();
    	repaint();
    	
    	if (!getDynamicTree().childrenAreLoaded(node)) {
        	// Load the children in the tree in case the user selects them in the gallery view
        	// TODO: this should pause the UI because it could cause a desync in theory, if it loads slower then 
        	// the user clicks.
	        SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, false);
	        loadingWorker.execute();
    	}
    }
}
