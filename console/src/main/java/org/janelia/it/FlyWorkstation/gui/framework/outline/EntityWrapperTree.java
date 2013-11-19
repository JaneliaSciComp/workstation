package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeLoader;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.entity.ForbiddenEntity;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.ConcurrentUtils;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

/**
 * A tree of Entity Wrappers that may load lazily. Manages all the asynchronous loading and tree updating that happens in the
 * case of lazy nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityWrapperTree extends JPanel implements ActivatableView {
	
	private static final Logger log = LoggerFactory.getLogger(EntityWrapperTree.class);
	
	private final JPanel treesPanel;
    private DynamicTree selectedTree;
    private boolean lazy;
    
	private EntityWrapper root;
    
	private Multimap<Long,DefaultMutableTreeNode> entityIdToNodeMap = HashMultimap.<Long,DefaultMutableTreeNode>create();
	private Map<String,DefaultMutableTreeNode> uniqueIdToNodeMap = new HashMap<String,DefaultMutableTreeNode>();
	
    public EntityWrapperTree() {
        this(false);
    }

    public EntityWrapperTree(boolean lazy) {
        super(new BorderLayout());
        
        this.lazy = lazy;
        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
    }

    @Override
    public void activate() {
        ModelMgr.getModelMgr().registerOnEventBus(this);
        refresh();
    }

    @Override
    public void deactivate() {
        ModelMgr.getModelMgr().unregisterOnEventBus(this);
    }
    
    public void initializeTree(final List<EntityWrapper> roots) {

        entityIdToNodeMap.clear();
        uniqueIdToNodeMap.clear();

        EntityType rootType = new EntityType();
        rootType.setName("");
        rootType.setAttributes(new HashSet<EntityAttribute>());
        
        Entity root = new Entity();
        root.setEntityType(rootType);
        root.setName("Data");
        
        EntityWrapper rootWrapper = new EntityWrapper(new RootedEntity(root)) {
            @Override
            public List<EntityWrapper> getChildren() {
                return roots;
            }
        };
        
        createNewTree(rootWrapper);
        
        int c = 0;
        for(EntityWrapper wrapper : roots) {
            addNode(selectedTree.getRootNode(), wrapper, c++); 
        }   
        
        showTree();
    }

    protected void createNewTree(EntityWrapper root) {

        this.root = root;
        selectedTree = new DynamicTree(root, false, lazy) {

            protected void showPopupMenu(MouseEvent e) {
                EntityWrapperTree.this.showPopupMenu(e);
            }

            protected void nodeClicked(MouseEvent e) {
                EntityWrapperTree.this.nodeClicked(e);
            }

            protected void nodePressed(MouseEvent e) {
                EntityWrapperTree.this.nodePressed(e);
            }

            protected void nodeDoubleClicked(MouseEvent e) {
                EntityWrapperTree.this.nodeDoubleClicked(e);
            }

            @Override
            public void expandNodeWithLazyChildren(final DefaultMutableTreeNode node, final Callable<Void> success) {
                
                EntityWrapper wrapper = getEntityWrapper(node);
                log.debug("expandNodeWithLazyChildren: {}",wrapper.getName());  
                
                SimpleWorker loadingWorker = new LazyTreeNodeLoader(selectedTree, node) {
                    protected void doneLoading() {
                        log.debug("expandNodeWithLazyChildren completed, from database: {}",getEntity(node).getName()); 
                        getDynamicTree().recreateChildNodes(node);
                        SwingUtilities.updateComponentTreeUI(EntityWrapperTree.this);
                        ConcurrentUtils.invokeAndHandleExceptions(success);
                    }
                };

                loadingWorker.execute();
            }

            @Override
            public void loadLazyNodeData(DefaultMutableTreeNode node) throws Exception {
                EntityWrapperTree.this.loadLazyNode(node);
            }

            @Override
            public void recreateChildNodes(DefaultMutableTreeNode node) {
                EntityWrapper wrapper = getEntityWrapper(node);
                log.debug("recreateChildNodes: {}",wrapper.getName());
                
                List<DefaultMutableTreeNode> childNodes = new ArrayList<DefaultMutableTreeNode>();
                for (int i = 0; i < node.getChildCount(); i++) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                    childNodes.add(childNode);
                }
                
                log.trace("Adding {} children",wrapper.getChildren().size());
                EntityWrapperTree.this.addChildren(node, wrapper.getChildren());
                
                // The old children (typically a LazyTreeNode) are not removed until after the new children are added
                // in order to avoid a flickering on the tree when opening a lazy node.
                
                log.trace("Removing {} children",childNodes.size());
                for(DefaultMutableTreeNode childNode : childNodes) {
                    EntityWrapperTree.this.removeNode(childNode);   
                }
            }

            @Override
            public String getUniqueId(DefaultMutableTreeNode node) {
                if (node==null) return null;
                if (node.isRoot()) return "/";
                EntityWrapper wrapper = getEntityWrapper(node);
                return wrapper.getUniqueId();
            }
            
            @Override
            public void navigateToNodeWithUniqueId(String uniqueId) {
                EntityWrapperTree.this.selectEntityByUniqueId(uniqueId);
            }
            
            @Override
            public void refresh() {
                EntityWrapperTree.this.refresh();
            }
            
            @Override
            public void totalRefresh() {
                EntityWrapperTree.this.totalRefresh();
            }
        };

        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityWrapperTreeCellRenderer());
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

//	@Subscribe 
//	public void entityChanged(EntityChangeEvent event) {
//		Entity entity = event.getEntity();
//		Collection<DefaultMutableTreeNode> nodes = getNodesByEntityId(entity.getId());
//		if (nodes == null) return;
//		log.debug("Entity affecting {} nodes was changed: '{}'",nodes.size(),entity.getName());	
//		
//		for(final DefaultMutableTreeNode node : new HashSet<DefaultMutableTreeNode>(nodes)) {
//			Entity treeEntity = getEntity(node);
//			if (entity!=treeEntity) {
//				log.warn("EntityOutline: Instance mismatch: "+entity.getName()+
//	    				" (cached="+System.identityHashCode(entity)+") vs (this="+System.identityHashCode(treeEntity)+")");
//				getEntityData(node).setChildEntity(entity);
//			}
//			log.debug("Recreating children of {}, {}",entity,getDynamicTree().getUniqueId(node));
//			getDynamicTree().recreateChildNodes(node); 
//		}
//	}
//
//	@Subscribe 
//	public void entityRemoved(EntityRemoveEvent event) {
//		Entity entity = event.getEntity();
//		Collection<DefaultMutableTreeNode> nodes = getNodesByEntityId(entity.getId());
//		if (nodes == null) return;
//		log.debug("Entity affecting {} nodes was removed: '{}'",nodes.size(),entity.getName());	
//		
//		for(DefaultMutableTreeNode node : new HashSet<DefaultMutableTreeNode>(nodes)) {
//			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
//			Entity parent = getEntity(parentNode);
//			EntityData entityData = getEntityData(node);
//			if (parent!=null) {
//				parent.getEntityData().remove(entityData);
//			}
//			removeNode(node);	
//		}
//	}
	
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

    /**
     * Override to provide custom selection behavior. 
     * @param uniqueId
     */
	public void selectEntityByUniqueId(String uniqueId) {
	}

    /**
     * Override to provide custom loading behavior. 
     * @param uniqueId
     */
    protected void loadLazyNode(DefaultMutableTreeNode node) throws Exception {
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
	
    
    private void addNode(DefaultMutableTreeNode parentNode, EntityWrapper entityWrapper, int index) {

        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, entityWrapper, index);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.getRootNode();
        }

    	log.trace("EntityWrapperTree.addNodes: {}, {}",entityWrapper.getName(),entityWrapper.getUniqueId());

        // Add to maps
        uniqueIdToNodeMap.put(entityWrapper.getUniqueId(), newNode);
        entityIdToNodeMap.put(entityWrapper.getId(), newNode);
        
        if (entityWrapper instanceof Neuron) {
            // Neurons cannot have children
            return;
        }
        
        // Add lazy child node
        selectedTree.addObject(newNode, new LazyTreeNode()); 
    }
    
    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityWrapper> childWrappers) {
    	return addChildren(parentNode, childWrappers, new HashSet<Long>(), 0);
    }
    
    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityWrapper> childWrappers, Set<Long> visitedEds, int level) {
        
        log.trace("EntityWrapperTree.addChildren - got childWrappers={} "+childWrappers);
        
		int c = 0;
        for(EntityWrapper childWrapper : childWrappers) {
            EntityData entityData = childWrapper.getInternalRootedEntity().getEntityData();
            if (EntityUtils.isHidden(entityData) || (entityData.getChildEntity() instanceof ForbiddenEntity) || !ModelMgrUtils.hasReadAccess(entityData.getChildEntity())) continue;
            addNode(parentNode, childWrapper, c++);    
        }
                
        return c;
    }

    protected synchronized void removeNode(DefaultMutableTreeNode node) {
    	
    	EntityWrapper wrapper = getEntityWrapper(node);
    	
    	if (node.getParent()==null) {
        	if (wrapper!=null) {
        		log.warn("EntityTree.removeNode: "+wrapper.getName()+" was already removed");	
        	}
    		return;
    	}
    	
    	removeChildren(node);
    	
    	if (wrapper!=null) {
            String uniqueId = selectedTree.getUniqueId(node);
        	log.trace("EntityTree.removeNode: {}, {}",wrapper.getName(),uniqueId);
        	
        	// Remove from all maps
        	if (uniqueIdToNodeMap.get(uniqueId)==node) {
        		uniqueIdToNodeMap.remove(uniqueId);	
        	}
            entityIdToNodeMap.remove(wrapper.getId(), node);
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

    /**
     * Get all the nodes in the tree with the given entity id.
     * @param entityId
     * @return
     */
    public Collection<DefaultMutableTreeNode> getNodesByEntityId(Long entityId) {
        return ImmutableList.copyOf(entityIdToNodeMap.get(entityId));
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

//    public EntityData getEntityDataByUniqueId(String uniqueId) {
//      DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
//      return getEntityData(node);
//    }
//    
//    public Entity getEntityByUniqueId(String uniqueId) {
//      DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
//      return getEntity(node);
//    }
//    
//    public Entity getParentEntityByUniqueId(String uniqueId) {
//      DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
//      if (node == null) return null;
//      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)node.getParent();
//      if (parentNode != null) {
//          return getEntity(parentNode);
//      }
//      return null;
//    }
//    
//    public Entity getCurrentRootEntity() {
//        return getEntity(selectedTree.getRootNode());
//    }
//
    public Entity getEntity(DefaultMutableTreeNode node) {
      EntityData ed = getEntityData(node);
      if (ed==null) return null;
      return ed.getChildEntity();
    }

    public EntityData getEntityData(DefaultMutableTreeNode node) {
      if (node==null) return null;
      EntityWrapper entityWrapper = getEntityWrapper(node);
      return (EntityData)entityWrapper.getInternalRootedEntity().getEntityData();
    }

    public EntityWrapper getEntityWrapper(DefaultMutableTreeNode node) {
        if (node==null) return null;
        return (EntityWrapper)node.getUserObject();
    }

    public EntityWrapper getRoot() {
        return root;
    }
    
    public DynamicTree getDynamicTree() {
        return selectedTree;
    }

    public JTree getTree() {
        if (selectedTree==null) return null;
        return selectedTree.getTree();
    }
}
