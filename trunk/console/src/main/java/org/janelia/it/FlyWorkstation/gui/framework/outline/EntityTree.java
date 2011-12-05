package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Cursor;
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
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

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
	private Entity rootEntity;
    
	private Map<Long,DefaultMutableTreeNode> entityIdToNodeMap = new HashMap<Long,DefaultMutableTreeNode>();
	
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
		return rootEntity;
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
        
        createNewTree(rootEntity);
        addNodes(null, rootEntity);

        selectedTree.expand(selectedTree.getRootNode(), true);

        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        EntityTree.this.updateUI();
    }

    public DynamicTree getDynamicTree() {
        return selectedTree;
    }

    public JTree getTree() {
        return selectedTree.getTree();
    }

    public Entity getCurrentRootEntity() {
        return (Entity) selectedTree.getRootNode().getUserObject();
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

    protected void createNewTree(Entity root) {

    	this.rootEntity = root;
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
                Entity entity = (Entity) node.getUserObject();
                
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
                
                Utils.loadLazyEntity(entity, recurse);
            }

            @Override
            public int recreateChildNodes(DefaultMutableTreeNode node) {
                Entity entity = (Entity) node.getUserObject();
                ArrayList<EntityData> edList = new ArrayList<EntityData>(entity.getOrderedEntityData());

                if (!Utils.areLoaded(edList)) {
                    throw new IllegalStateException("replaceLazyChildren called on node whose children have not been loaded");
                }

                selectedTree.removeChildren(node);
                return addChildren(node, edList);
            }

            @Override
            public void expandAll(final boolean expand) {

                if (!expand || !isLazyLoading()) {
                    super.expandAll(expand);
                    return;
                }

                if (loadingWorker != null && !loadingWorker.isDone()) {
                	loadingWorker.cancel(true);
                }
                
                // Expanding a lazy tree is hard. Let's eager load the entire thing first.

                progressMonitor = new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Loading tree...", "", 0, 100);
                progressMonitor.setProgress(0);
                progressMonitor.setMillisToDecideToPopup(0);
                
                loadingWorker = new FakeProgressWorker() {

                    private Entity rootEntity;

                    @Override
                    protected void doStuff() throws Exception {
                    	progressMonitor.setProgress(1);
                        long rootId = ((Entity) getRootNode().getUserObject()).getId();
                        rootEntity = ModelMgr.getModelMgr().getEntityTree(rootId);
                    }

                    @Override
                    protected void hadSuccess() {
                    	if (isCancelled()) return;
                    	if (getProgress() < 90) setProgress(90);
                        // The tree is no longer lazy
                        setLazyLoading(false);
                        DefaultMutableTreeNode rootNode = getRootNode();
                        rootNode.setUserObject(rootEntity);
                        recreateChildNodes(rootNode, true);
                        expandAll(new TreePath(rootNode), expand);
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
                        if (getProgress() < 100) setProgress(100);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        getDynamicTree().setCursor(Cursor.getDefaultCursor());
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
    
    public DefaultMutableTreeNode getNodeByEntityId(Long entityId) {
    	return entityIdToNodeMap.get(entityId);
    }

    public Entity getEntityById(Long entityId) {
    	DefaultMutableTreeNode node = entityIdToNodeMap.get(entityId);
    	if (node==null) return null;
    	return (Entity)node.getUserObject();
    }
    

    private void addNodes(DefaultMutableTreeNode parentNode, Entity newEntity) {

        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, newEntity);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.getRootNode();
        }
        
        entityIdToNodeMap.put(newEntity.getId(), newNode);
        
        List<EntityData> dataList = newEntity.getOrderedEntityData();
        List<EntityData> childDataList = new ArrayList<EntityData>();

        for (EntityData ed : dataList) {
        	if (ed.getChildEntity()!=null) {
        		childDataList.add(ed);
        	}
        }

        if (childDataList.isEmpty()) return;

        // Test for proxies
        boolean allLoaded = Utils.areLoaded(childDataList);

        if (!allLoaded) {
            selectedTree.addObject(newNode, new LazyTreeNode());
        }
        else {
            addChildren(newNode, childDataList);
        }

    }

    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityData> dataList) {

        int c = 0;
        for (EntityData entityData : dataList) {
            Entity child = entityData.getChildEntity();
            if (child != null) {
                addNodes(parentNode, child);
                c++;
            }
        }
        return c;
    }
}
