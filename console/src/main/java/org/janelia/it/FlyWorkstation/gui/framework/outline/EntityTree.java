package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityChildrenLoadedEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.events.EntityRemoveEvent;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.util.FakeProgressWorker;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

/**
 * A tree of Entities that may load lazily. Manages all the asynchronous loading and tree updating that happens in the
 * case of lazy nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTree extends JPanel {
	
	private static final Logger log = LoggerFactory.getLogger(EntityTree.class);
	
    protected final JPanel treesPanel;
    protected DynamicTree selectedTree;
    protected boolean lazy;
    
    private SimpleWorker loadingWorker;
	private EntityData rootEntityData;
    
	private Multimap<Long,DefaultMutableTreeNode> entityDataIdToNodeMap = HashMultimap.<Long,DefaultMutableTreeNode>create();
	private Multimap<Long,DefaultMutableTreeNode> entityIdToNodeMap = HashMultimap.<Long,DefaultMutableTreeNode>create();
	private Map<String,DefaultMutableTreeNode> uniqueIdToNodeMap = new HashMap<String,DefaultMutableTreeNode>();
	
    public EntityTree() {
        this(false);
    }

    public EntityTree(boolean lazy) {
        super(new BorderLayout());
        
        this.lazy = lazy;
        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
        
        ModelMgr.getModelMgr().registerOnEventBus(this);
    }

    public Entity getRootEntity() {
    	if (rootEntityData==null) return null;
		return rootEntityData.getChildEntity();
	}

	public void showNothing() {
        treesPanel.removeAll();
        revalidate();
        repaint();
    }
    
    public void showLoadingIndicator() {
        treesPanel.removeAll();
        treesPanel.add(new JLabel(Icons.getLoadingIcon()));
        revalidate();
        repaint();
    }

    public void showTree() {
        treesPanel.removeAll();
        treesPanel.add(selectedTree);
        revalidate();
        repaint();
    }
    
    public void initializeTree(final Long rootId, final Callable<Void> success) {
        if (null != selectedTree) {
            ToolTipManager.sharedInstance().unregisterComponent(selectedTree);
        }

        if (rootId == null) return;

        SimpleWorker loadingWorker = new SimpleWorker() {

            private Entity rootEntity;

            protected void doStuff() throws Exception {
                if (lazy) {
                    rootEntity = ModelMgr.getModelMgr().getEntityById(rootId.toString());
                }
                else {
                    rootEntity = ModelMgr.getModelMgr().getEntityTree(rootId);
                }
            }

            protected void hadSuccess() {
                try {
                    initializeTree(rootEntity);
                    if (null != selectedTree) {
                        ToolTipManager.sharedInstance().registerComponent(selectedTree);
                    }
                    if (success!=null) success.call();
                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(EntityTree.this, "Error loading folders", "Folder Load Error", JOptionPane.ERROR_MESSAGE);
                showNothing();
            }

        };

        loadingWorker.execute();
    }

    public void initializeTree(final Entity rootEntity) {

    	entityDataIdToNodeMap.clear();
        entityIdToNodeMap.clear();
        uniqueIdToNodeMap.clear();
        
        // Dummy ed for the root
        EntityData rootEd = new EntityData();
        rootEd.setChildEntity(rootEntity);

        createNewTree(rootEd);
        
        addNodes(null, rootEd);

        showTree();
    }

	@Subscribe 
	public void entityChanged(EntityChangeEvent event) {
		Entity entity = event.getEntity();
		Collection<DefaultMutableTreeNode> nodes = getNodesByEntityId(entity.getId());
		if (nodes == null) return;
		log.debug("Entity affecting {} nodes was changed: '{}'",nodes.size(),entity.getName());	
		
		for(final DefaultMutableTreeNode node : new HashSet<DefaultMutableTreeNode>(nodes)) {
			Entity treeEntity = getEntity(node);
			if (entity!=treeEntity) {
				log.warn("EntityOutline: Instance mismatch: "+entity.getName()+
	    				" (cached="+System.identityHashCode(entity)+") vs (this="+System.identityHashCode(treeEntity)+")");
				getEntityData(node).setChildEntity(entity);
			}
			log.debug("Recreating children of {}, {}",entity,getDynamicTree().getUniqueId(node));
			getDynamicTree().recreateChildNodes(node); 
		}
	}

	@Subscribe 
	public void entityRemoved(EntityRemoveEvent event) {
		Entity entity = event.getEntity();
		Collection<DefaultMutableTreeNode> nodes = getNodesByEntityId(entity.getId());
		if (nodes == null) return;
		log.debug("Entity affecting {} nodes was removed: '{}'",nodes.size(),entity.getName());	
		
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

	@Subscribe 
	public void entityChildrenLoaded(EntityChildrenLoadedEvent event) {
		Entity entity = event.getEntity();
		Collection<DefaultMutableTreeNode> nodes = getNodesByEntityId(entity.getId());
		if (nodes == null) return;
		log.debug("Entity affecting {} nodes had children loaded: '{}'",nodes.size(),entity.getName());	
		for(DefaultMutableTreeNode node : nodes) {
			log.debug("Recreating children of {}",getDynamicTree().getUniqueId(node));
			getDynamicTree().recreateChildNodes(node);	
		}
	}
	
    public DynamicTree getDynamicTree() {
        return selectedTree;
    }

    public JTree getTree() {
    	if (selectedTree==null) return null;
        return selectedTree.getTree();
    }

    public Entity getCurrentRootEntity() {
        return getEntity(selectedTree.getRootNode());
    }

    public Entity getEntity(DefaultMutableTreeNode node) {
    	EntityData ed = getEntityData(node);
    	if (ed==null) return null;
    	return ed.getChildEntity();
    }

    public EntityData getEntityData(DefaultMutableTreeNode node) {
    	if (node==null) return null;
    	return (EntityData)node.getUserObject();
    }
    
    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user left clicks a node.
     *
     * @param e
     */
    protected void nodeClicked(MouseEvent e) {
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

    protected void createNewTree(EntityData root) {

    	this.rootEntityData = root;
        selectedTree = new DynamicTree(root, true, lazy) {

            protected void showPopupMenu(MouseEvent e) {
                EntityTree.this.showPopupMenu(e);
            }

            protected void nodeClicked(MouseEvent e) {
                EntityTree.this.nodeClicked(e);
            }

            protected void nodePressed(MouseEvent e) {
                EntityTree.this.nodePressed(e);
            }

            protected void nodeDoubleClicked(MouseEvent e) {
                EntityTree.this.nodeDoubleClicked(e);
            }

            @Override
            public void expandNodeWithLazyChildren(final DefaultMutableTreeNode node, final Callable<Void> success) {
            	
            	log.debug("expandNodeWithLazyChildren {}",getEntity(node).getName());	
            	
            	if (EntityUtils.areLoaded(getEntity(node).getEntityData())) {
            		SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
		                	log.debug("expandNodeWithLazyChildren completed, from cache: {}",getEntity(node).getName());	
		        			getDynamicTree().recreateChildNodes(node);
		                    SwingUtilities.updateComponentTreeUI(EntityTree.this);
		                    if (success!=null) {
		                    	try {
		                    		success.call();
		                    	}
		                    	catch (Exception e) {
		                    		SessionMgr.getSessionMgr().handleException(e);
		                    	}
		                    }
						}
					});
            		return;
            	}
            	
                SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node) {
                    protected void doneLoading() {
                    	log.debug("expandNodeWithLazyChildren completed, from database: {}",getEntity(node).getName());	
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
	                    if (success!=null) {
	                    	try {
	                    		success.call();
	                    	}
	                    	catch (Exception e) {
	                    		SessionMgr.getSessionMgr().handleException(e);
	                    	}
	                    }
                    }
                };

                loadingWorker.execute();
            }

            @Override
            public void loadLazyNodeData(DefaultMutableTreeNode node) throws Exception {
                Entity entity = getEntity(node);
            	ModelMgr.getModelMgr().loadLazyEntity(entity, false);
            }

            @Override
            public void recreateChildNodes(DefaultMutableTreeNode node) {
                Entity entity = getEntity(node);
                log.debug("recreateChildNodes {}",entity);
                
                ArrayList<EntityData> edList = new ArrayList<EntityData>(entity.getOrderedEntityData());

            	List<DefaultMutableTreeNode> childNodes = new ArrayList<DefaultMutableTreeNode>();
                for (int i = 0; i < node.getChildCount(); i++) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                    childNodes.add(childNode);
                }
                
                log.debug("Adding {} children",edList.size());
                EntityTree.this.addChildren(node, edList);

                
                // The old children (typically a LazyTreeNode) are not removed until after the new children are added
                // in order to avoid a flickering on the tree when opening a lazy node.
                
                log.debug("Removing {} children",childNodes.size());
                for(DefaultMutableTreeNode childNode : childNodes) {
                	EntityTree.this.removeNode(childNode);	
                }
            }

            @Override
            public void expandAll(final DefaultMutableTreeNode node, final boolean expand) {

                if (!expand || !isLazyLoading()) {
                    super.expandAll(node, expand);
                    return;
                }

                if (loadingWorker != null && !loadingWorker.isDone()) {
                	loadingWorker.cancel(true);
                }
                
                // Expanding a lazy tree node-by-node takes forever. Let's eager load the entire thing first.

                Utils.setWaitingCursor(EntityTree.this);
                
                loadingWorker = new FakeProgressWorker() {

                    private EntityData entityData;
                    private Entity entity;

                    @Override
                    protected void doStuff() throws Exception {
                    	progressMonitor.setProgress(1);
                    	entityData = getEntityData(node);
                        entity = ModelMgr.getModelMgr().getEntityTree(entityData.getChildEntity().getId());
                    }

                    @Override
                    protected void hadSuccess() {
                    	if (isCancelled()) return;
                    	if (getProgress() < 90) setProgress(90);
                    	entityData.setChildEntity(entity);
                        node.setUserObject(entityData);
                        recreateChildNodes(node);
                        expandAll(new TreePath(node.getPath()), expand);
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
                        if (getProgress() < 100) setProgress(100);
                        Utils.setDefaultCursor(EntityTree.this);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                    	Utils.setDefaultCursor(EntityTree.this);
                    	SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                
                loadingWorker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Loading tree...", "", 0, 100));
                loadingWorker.execute();
            }

			@Override
            public String getUniqueId(DefaultMutableTreeNode node) {
				if (node==null) return null;
				if (node.isRoot()) return "/";
		    	StringBuffer sb = new StringBuffer();
		    	DefaultMutableTreeNode curr = node;
		    	while(curr != null) {
		    		if (node != curr) sb.insert(0, "/");
		    		EntityData ed = getEntityData(curr);
		    		if (ed==null) {
		    			log.warn("Encountered null EntityData while building unique id: "+sb);
		    			return null;
		    		}
		    		String nodeId = ed.getId()==null ? "" : "ed_"+ed.getId();
		    		nodeId += "/" + "e_"+ed.getChildEntity().getId();
					sb.insert(0, nodeId);
		    		curr = ed.getId()==null ? null : (DefaultMutableTreeNode)curr.getParent();
		    	}
		    	return sb.toString();
            }
			
			@Override
			public void navigateToNodeWithUniqueId(String uniqueId) {
				EntityTree.this.selectEntityByUniqueId(uniqueId);
			}
            
			@Override
			public void refresh() {
				EntityTree.this.refresh();
			}
			
			@Override
			public void totalRefresh() {
				EntityTree.this.totalRefresh();
			}
        };

        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityTreeCellRenderer());
    }

    /**
     * Override to provide custom selection behavior. 
     * @param uniqueId
     */
	public void selectEntityByUniqueId(String uniqueId) {
	}
	
	/**
	 * Override to provide refresh behavior. Default implementation does nothing.
	 */
	public void refresh() {
    }

	/**
	 * Override to provide total refresh behavior. Default implementation does nothing.
	 */
	public void totalRefresh() {
    }
	
    /**
     * Get all the entity objects in the tree with the given id.
     * @param entityId
     * @return
     */
    public Set<Entity> getEntitiesById(Long entityId) {
    	Collection<DefaultMutableTreeNode> nodes = entityIdToNodeMap.get(entityId);
    	Set<Entity> entities = new HashSet<Entity>();
    	if (nodes==null) return entities;
    	for(DefaultMutableTreeNode node : nodes) {
    		entities.add(getEntity(node));
    	}
    	return entities;
    }

    /**
     * Get all the entity data objects in the tree with the given id.
     * @param entityId
     * @return
     */
    public Set<EntityData> getEntityDatasById(Long entityDataId) {
    	Collection<DefaultMutableTreeNode> nodes = entityDataIdToNodeMap.get(entityDataId);
    	Set<EntityData> entityDatas = new HashSet<EntityData>();
    	if (nodes==null) return entityDatas;
    	for(DefaultMutableTreeNode node : nodes) {
    		entityDatas.add(getEntityData(node));
    	}
    	return entityDatas;
    }

    /**
     * Get all the nodes in the tree with the given entity id.
     * @param entityId
     * @return
     */
    public Collection<DefaultMutableTreeNode> getNodesByEntityId(Long entityId) {
    	return ImmutableList.copyOf(entityIdToNodeMap.get(entityId));
//    	return new ArrayList<DefaultMutableTreeNode>(entityIdToNodeMap.get(entityId));
    }
    
    /**
     * Get all the nodes in the tree with the given entity data id.
     * @param entityDataId
     * @return
     */
    public Collection<DefaultMutableTreeNode> getNodesByEntityDataId(Long entityDataId) {
    	return entityDataIdToNodeMap.get(entityDataId);
    }

	public static String getChildUniqueId(String parentUniqueId, EntityData entityData) {
		String uniqueId = parentUniqueId;
		uniqueId += "/ed_"+entityData.getId();
		uniqueId += "/e_"+entityData.getChildEntity().getId();
		return uniqueId;
	}

    public String getCurrUniqueId() {
    	return getDynamicTree().getUniqueId(getDynamicTree().getCurrentNode());
    }

    public DefaultMutableTreeNode getNodeByUniqueId(String uniqueId) {
    	return uniqueIdToNodeMap.get(uniqueId);
    }

    public EntityData getEntityDataByUniqueId(String uniqueId) {
    	DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
    	return getEntityData(node);
    }
    
    public Entity getEntityByUniqueId(String uniqueId) {
    	DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
    	return getEntity(node);
    }
    
    public Entity getParentEntityByUniqueId(String uniqueId) {
    	DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
    	if (node == null) return null;
    	DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)node.getParent();
    	if (parentNode != null) {
    		return getEntity(parentNode);
    	}
    	return null;
    }

    protected void addNodes(DefaultMutableTreeNode parentNode, EntityData newEd) {
    	addNodes(parentNode, newEd, new HashSet<Long>(), 0);
    }
    		
    private void addNodes(DefaultMutableTreeNode parentNode, EntityData newEd, Set<Long> visitedEds, int level) {
    	if (parentNode==null) {
        	addNodes(parentNode, newEd, 0);
    	}
    	else {
    		addNodes(parentNode, newEd, parentNode.getChildCount());	
    	}
    }

    protected void addNodes(DefaultMutableTreeNode parentNode, EntityData newEd, int index) {
    	addNodes(parentNode, newEd, index, new HashSet<Long>(), 0);
    }
    
    private void addNodes(DefaultMutableTreeNode parentNode, EntityData newEd, int index, Set<Long> visitedEds, int level) {

    	StringBuffer indent = new StringBuffer();
    	for(int i=0; i<level; i++) {
    		indent.append("    ");
    	}
    	
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, newEd, index);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.getRootNode();
        }

        Entity entity = newEd.getChildEntity();
        String uniqueId = selectedTree.getUniqueId(newNode);
    	log.debug("EntityTree.addNodes: {}, {}",entity,uniqueId);

        // Add to maps
        uniqueIdToNodeMap.put(uniqueId, newNode);
        entityIdToNodeMap.put(entity.getId(), newNode);
        entityDataIdToNodeMap.put(newEd.getId(), newNode);
        
        // Get children
        
        List<EntityData> dataList = entity.getOrderedEntityData();
        List<EntityData> childDataList = new ArrayList<EntityData>();

        boolean allHidden = true;
        
        for (EntityData ed : dataList) {
        	if (ed.getChildEntity()!=null) {
        		childDataList.add(ed);
        		if (!EntityUtils.isHidden(ed)) allHidden = false;
        	}
        }

        if (childDataList.isEmpty() || allHidden) return;
        
        // End infinite recursion

    	Set<Long> nextVisitedEds = new HashSet<Long>(visitedEds);
    	
    	if (newEd.getId()!=null) {
	    	if (visitedEds.contains(newEd.getId())) {
	    		if (!childDataList.isEmpty()) {
//	    			System.out.println(indent+"EntityTree.addNodes - add lazy node to "+getEntity(parentNode).getName());
	    			selectedTree.addObject(parentNode, new LazyTreeNode());	
	    		}
//	    		System.out.println(indent+"EntityTree.addNodes - already been at "+entity.getName()+" ("+newEd.getId()+")");
	    		return;
	    	}
	    	nextVisitedEds.add(newEd.getId());
    	}
    	
    	// Add children
        
        addChildren(newNode, childDataList, nextVisitedEds, level);
    }

    protected synchronized void removeNode(DefaultMutableTreeNode node) {
    	
    	EntityData entityData = getEntityData(node);
    	Entity entity = getEntity(node);
    	
    	if (node.getParent()==null) {
        	if (entity!=null) {
        		log.warn("EntityTree.removeNode: "+entity.getName()+" was already removed");	
        	}
    		return;
    	}
    	
    	removeChildren(node);
    	
    	if (entityData!=null && entity!=null) {
            String uniqueId = selectedTree.getUniqueId(node);
        	log.debug("EntityTree.removeNode: {}, {}",entity,uniqueId);
        	
        	// Remove from all maps
//            uniqueIdToNodeMap.remove(uniqueId);
            entityIdToNodeMap.remove(entity.getId(), node);
            entityDataIdToNodeMap.remove(entity.getId(), node);
    	}
    	
        // Remove from the tree
        getDynamicTree().removeNode(node);
    }
    
    public void removeChildren(DefaultMutableTreeNode node) {
    	List<DefaultMutableTreeNode> childNodes = new ArrayList<DefaultMutableTreeNode>();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            childNodes.add(childNode);
        }
        for(DefaultMutableTreeNode childNode : childNodes) {
        	removeNode(childNode);	
        }
    }
    
    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityData> dataList) {
    	return addChildren(parentNode, dataList, new HashSet<Long>(), 0);
    }
    
    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityData> dataList, Set<Long> visitedEds, int level) {

    	StringBuffer indent = new StringBuffer();
    	for(int i=0; i<level; i++) {
    		indent.append("    ");
    	}
    	
//		System.out.println(indent+"EntityTree.addChildren - add to "+getEntityData(parentNode));

        // Test for proxies
        if (!EntityUtils.areLoaded(dataList)) {
            selectedTree.addObject(parentNode, new LazyTreeNode());
            return 1;
        }
    	
        int c = 0;
        for (EntityData entityData : dataList) {
            if (entityData.getChildEntity() != null) {
            	if (EntityUtils.isHidden(entityData)) continue;
                addNodes(parentNode, entityData, c++, visitedEds, level+1);
            }
        }
                
        return c;
    }
}
