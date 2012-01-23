package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.util.FakeProgressWorker;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A tree of Entities that may load lazily. Manages all the asynchronous loading and tree updating that happens in the
 * case of lazy nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTree extends JPanel implements PropertyChangeListener  {

    protected final JPanel treesPanel;
    protected DynamicTree selectedTree;
    protected boolean lazy;
    
    private FakeProgressWorker loadingWorker;
    private ProgressMonitor progressMonitor;
	private EntityData rootEntityData;
    
	private Map<Long,Set<DefaultMutableTreeNode>> entityIdToNodeMap = new HashMap<Long,Set<DefaultMutableTreeNode>>();
	private Map<String,DefaultMutableTreeNode> uniqueIdToNodeMap = new HashMap<String,DefaultMutableTreeNode>();
	
    public EntityTree() {
        this(false);
    }

    public EntityTree(boolean lazy) {
        super(new BorderLayout());
        
        this.lazy = lazy;
        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
    }

    public Entity getRootEntity() {
		return rootEntityData.getChildEntity();
	}

	public void showNothing() {
        treesPanel.removeAll();
    }
    
    public void showLoadingIndicator() {
        treesPanel.removeAll();
        treesPanel.add(new JLabel(Icons.getLoadingIcon()));
    }

    public void initializeTree(final Long rootId, final Callable<Void> success) {
        if (null != selectedTree) {
            ToolTipManager.sharedInstance().unregisterComponent(selectedTree);
        }
        
        treesPanel.removeAll();

        if (rootId == null) return;

        treesPanel.add(new JLabel(Icons.getLoadingIcon()));
        this.updateUI();

        SimpleWorker loadingWorker = new SimpleWorker() {

            private Entity rootEntity;

            protected void doStuff() throws Exception {
                if (lazy) {
                    rootEntity = ModelMgr.getModelMgr().getEntityById(rootId.toString());
                }
                else {
                    rootEntity = ModelMgr.getModelMgr().getCachedEntityTree(rootId);
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
                treesPanel.removeAll();
                EntityTree.this.updateUI();
            }

        };

        loadingWorker.execute();
    }

    public void initializeTree(final Entity rootEntity) {

        entityIdToNodeMap.clear();
        uniqueIdToNodeMap.clear();
        
        // Dummy ed for the root
        EntityData rootEd = new EntityData();
        rootEd.setChildEntity(rootEntity);

        createNewTree(rootEd);
        
        addNodes(null, rootEd);

        selectedTree.expand(selectedTree.getRootNode(), true);

        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        EntityTree.this.updateUI();
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
            public void expandNodeWithLazyChildren(final DefaultMutableTreeNode node) {

                SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, false) {
                    protected void doneLoading() {
                        // Re-expand the node because the model was updated
                        expand(node, true);
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
                    }
                };

                loadingWorker.execute();
            }

            @Override
            public void loadLazyNodeData(DefaultMutableTreeNode node, boolean recurse) throws Exception {
                Entity entity = getEntity(node);
                
                if (recurse == true) {
                	// It's much faster to load the entire subtree in one go
            	
                	Entity fullEntity = ModelMgr.getModelMgr().getEntityTree(entity.getId());	

                    Map<Long, Entity> childEntityMap = new HashMap<Long, Entity>();
                    for (EntityData ed : fullEntity.getEntityData()) {
                    	Entity childEntity = ed.getChildEntity();
                    	if (childEntity == null) continue;
                        childEntityMap.put(childEntity.getId(), childEntity);
                    }

                    // Replace the entity data with real objects
                    for (EntityData ed : entity.getEntityData()) {
                        if (ed.getChildEntity() != null) {
                            ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
                        }
                    }
            	
                	return;
                }
                
                ModelMgrUtils.loadLazyEntity(entity, recurse);
            }

            @Override
            public int recreateChildNodes(DefaultMutableTreeNode node) {
                Entity entity = getEntity(node);
                ArrayList<EntityData> edList = new ArrayList<EntityData>(entity.getOrderedEntityData());
                selectedTree.removeChildren(node);
                return addChildren(node, edList);
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

                progressMonitor = new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Loading tree...", "", 0, 100);
                progressMonitor.setProgress(0);
                progressMonitor.setMillisToDecideToPopup(0);

                Utils.setWaitingCursor(EntityTree.this);
                
                loadingWorker = new FakeProgressWorker() {

                    private Entity entity;

                    @Override
                    protected void doStuff() throws Exception {
                    	progressMonitor.setProgress(1);
                        long entityId = getEntity(node).getId();
                        entity = ModelMgr.getModelMgr().getEntityTree(entityId);
                    }

                    @Override
                    protected void hadSuccess() {
                    	if (isCancelled()) return;
                    	if (getProgress() < 90) setProgress(90);
                        node.setUserObject(entity);
                        recreateChildNodes(node);
                        expandAll(new TreePath(node.getPath()), expand);
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
                        if (getProgress() < 100) setProgress(100);
                        Utils.setDefaultCursor(EntityTree.this);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                    	Utils.setDefaultCursor(EntityTree.this);
                        JOptionPane.showMessageDialog(EntityTree.this, "Error loading tree", "Internal Error", JOptionPane.ERROR_MESSAGE);
                    }
                };
                
                loadingWorker.addPropertyChangeListener(EntityTree.this);
                loadingWorker.executeWithProgress();
            }

			@Override
			public void refresh() {
				EntityTree.this.refresh();
			}
        };

        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityTreeCellRenderer());
    }
    
    protected void refresh() {
    }
    
    /**
     * Invoked when the loader's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if ("progress".equals(e.getPropertyName())) {
            int progress = (Integer) e.getNewValue();
            progressMonitor.setProgress(progress);
            String message = String.format("Completed %d%%", progress);
            progressMonitor.setNote(message);
            if (progressMonitor.isCanceled()) {
            	loadingWorker.cancel(true);
            }
        }
    }


    /**
     * Returns the full path to the given node
     * @param node
     * @return
     */
    public String getIdPath(DefaultMutableTreeNode node) {
    	StringBuffer sb = new StringBuffer();
    	DefaultMutableTreeNode curr = node;
    	while(curr != null) {
    		if (node != curr) sb.insert(0, "/");
    		EntityData ed = getEntityData(curr);
    		String nodeId = ed.getId()==null ? "" : "ed_"+ed.getId();
    		nodeId += "/" + "e_"+ed.getChildEntity().getId();
			sb.insert(0, nodeId);
    		curr = ed.getId()==null ? null : (DefaultMutableTreeNode)curr.getParent();
    	}
    	return sb.toString();
    }
    
    /**
     * Returns the unique id based on the path to the given node
     * @param node
     * @return
     */
    public String getUniqueId(DefaultMutableTreeNode node) {
    	// TODO: in the future, maybe hash this and cache it to save memory
    	return getIdPath(node);
    }

    /**
     * Get all the entity objects in the tree with the given id.
     * @param entityId
     * @return
     */
    public Set<Entity> getEntitiesById(Long entityId) {
    	Set<DefaultMutableTreeNode> nodes = entityIdToNodeMap.get(entityId);
    	Set<Entity> entities = new HashSet<Entity>();
    	for(DefaultMutableTreeNode node : nodes) {
    		entities.add(getEntity(node));
    	}
    	return entities;
    }
    
    public DefaultMutableTreeNode getChildWithEntity(Long entityId) {
    	
    	try {
    		DynamicTree dynamicTree = getDynamicTree();
        	DefaultMutableTreeNode node = dynamicTree.getCurrentNode();
        	if (node==null) return null;
//        	if (!dynamicTree.childrenAreLoaded(node)) {
//                SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node, false) {
//
//                    protected void doneLoading() {
//                    }
//
//                    @Override
//                    protected void hadError(Throwable error) {
//                    	SessionMgr.getSessionMgr().handleException(error);
//                    }
//                };
//
//                loadingWorker.execute();
//                loadingWorker.get();
//        	}
        	
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                Entity entity = getEntity(childNode);
                if (entity!=null && entity.getId().equals(entityId)) {
                	return childNode;
                }
            }
    	}
    	catch (Exception e) {
    		SessionMgr.getSessionMgr().handleException(e);
    	}
    	return null;
    }

    public String getChildUniqueIdWithEntity(Long entityId) {
    	return getUniqueId(getChildWithEntity(entityId));
    }

    public String getCurrUniqueId() {
    	return getUniqueId(getDynamicTree().getCurrentNode());
    }

   
    public DefaultMutableTreeNode getNodeByUniqueId(String uniqueId) {
    	return uniqueIdToNodeMap.get(uniqueId);
    }

    public Entity getEntityByUniqueId(String uniqueId) {
    	DefaultMutableTreeNode node = uniqueIdToNodeMap.get(uniqueId);
    	if (node==null) return null;
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
    	if (parentNode==null) {
        	addNodes(parentNode, newEd, 0);
    	}
    	else {
    		addNodes(parentNode, newEd, parentNode.getChildCount());	
    	}
    }

    protected void addNodes(DefaultMutableTreeNode parentNode, EntityData newEd, int index) {

        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, newEd, index);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.getRootNode();
        }
        
        Entity entity = newEd.getChildEntity();
        
        // Add to unique map
        uniqueIdToNodeMap.put(getUniqueId(newNode), newNode);
        
        // Add to duplicate map
        Set<DefaultMutableTreeNode> nodes = entityIdToNodeMap.get(entity.getId());
        if (nodes==null) {
        	nodes = new HashSet<DefaultMutableTreeNode>();
        	entityIdToNodeMap.put(entity.getId(), nodes);
        }
        nodes.add(newNode);
        
        List<EntityData> dataList = entity.getOrderedEntityData();
        List<EntityData> childDataList = new ArrayList<EntityData>();

        for (EntityData ed : dataList) {
        	if (ed.getChildEntity()!=null) {
        		childDataList.add(ed);
        	}
        }

        if (childDataList.isEmpty()) return;
        
        addChildren(newNode, childDataList);
    }

    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityData> dataList) {
    	
        // Test for proxies
        boolean allLoaded = EntityUtils.areLoaded(dataList);

        if (!allLoaded) {
            selectedTree.addObject(parentNode, new LazyTreeNode());
            return 1;
        }
    	
        int c = 0;
        for (EntityData entityData : dataList) {
            if (entityData.getChildEntity() != null) {
                addNodes(parentNode, entityData);
                c++;
            }
        }
                
        return c;
    }
}
