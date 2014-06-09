package org.janelia.it.workstation.gui.framework.outline;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.workstation.api.entity_model.events.EntityChangeEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityCreateEvent;
import org.janelia.it.workstation.api.entity_model.events.EntityInvalidationEvent;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.ScreenEvaluationDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tree.ExpansionState;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.utils.ModelUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.dialogs.SetSortCriteriaDialog;
import org.janelia.it.workstation.shared.workers.IndeterminateProgressMonitor;

/**
 * The entity tree which lives in the right-hand "Data" panel and drives the viewers.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityOutline extends EntityTree implements Refreshable, ActivatableView {

    private static final Logger log = LoggerFactory.getLogger(EntityOutline.class);

    protected Entity root;
    protected List<Entity> entityRootList;
    protected ModelMgrAdapter mml;
    protected String currUniqueId;

    public EntityOutline() {

        setMinimumSize(new Dimension(0, 0));

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

        if (entityRootList == null || entityRootList.isEmpty()) {
            this.root = new Entity();
            root.setEntityTypeName("");
            root.setName("No data");
            initializeTree(root);
        }
        else {
            // TODO: allow the user to choose the workspace. For now there is just one.
            try {
                this.root = ModelMgr.getModelMgr().getCurrentWorkspace();
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
            initializeTree(root);
        }
    }

    @Override
    public void initializeTree(Entity rootEntity) {
        super.initializeTree(rootEntity);

        selectedTree.expand(selectedTree.getRootNode(), true);
        selectedTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);

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

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enterAction");
        getActionMap().put("enterAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getCurrUniqueId() != null) {
                    selectEntityByUniqueId(getCurrUniqueId());
                }
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

        public EntityOutlineContextMenu(List<DefaultMutableTreeNode> nodes) {
            List<RootedEntity> rootedEntities = new ArrayList<RootedEntity>();
            for(DefaultMutableTreeNode node : nodes) {
                rootedEntities.add(new RootedEntity(selectedTree.getUniqueId(node), getEntityData(node)));
            }
            
            if (rootedEntities.isEmpty()) {
                rootedEntities.add(new RootedEntity(getRootUniqueId(), getRootEntityData()));
            }
            
            init(rootedEntities);
        }

        public void addRootMenuItems() {     
            add(getRootItem());   
            add(getSetSortCriteriaItem());
            add(getNewRootFolderItem());
            add(getWrapperCreatorItem());
            for (JComponent item : getOpenForContextItems()) {
                add(item);
            }
        }

        protected JMenuItem getRootItem() {
            JMenuItem titleMenuItem = new JMenuItem(getRootEntity().getName());
            titleMenuItem.setEnabled(false);
            return titleMenuItem;
        }

        protected JMenuItem getSetSortCriteriaItem() {

            if (multiple) {
                return null;
            }

            JMenuItem sortItem = new JMenuItem("  Set Sorting Criteria");

            sortItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        SetSortCriteriaDialog dialog = new SetSortCriteriaDialog();
                        dialog.showForEntity(getRootEntity());
                    } 
                    catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            });

            return sortItem;
        }
        
        private JMenuItem getNewRootFolderItem() {
            if (multiple) {
                return null;
            }

            JMenuItem newFolderItem = new JMenuItem("  Create New Top-Level Folder");
            newFolderItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {

                    // Add button clicked
                    final String folderName = (String) JOptionPane.showInputDialog(SessionMgr.getMainFrame(), "Folder Name:\n",
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
                                    EntityData ed = EntityUtils.findChildEntityDataWithChildId(root, newFolder.getId());
                                    if (ed==null) {
                                        log.error("Could not find newly created child of "+root.getId()+" with id "+newFolder.getId());
                                        return;
                                    }
                                    selectEntityByUniqueId(ModelUtils.getChildUniqueId(getRootUniqueId(), ed));
                                }
                            });
                        }

                        @Override
                        protected void hadError(Throwable error) {
                            SessionMgr.getSessionMgr().handleException(error);
                        }
                    };
                    worker.setProgressMonitor(new IndeterminateProgressMonitor(mainFrame, "Adding new top-level folder...", ""));
                    worker.execute();
                }
            });

            return newFolderItem;
        }

        public JMenuItem getWrapperCreatorItem() {
            if (multiple) {
                return null;
            }
            return new WrapperCreatorItemFactory().makeEntityWrapperCreatorItem(null);
        }
    }

    /**
     * Override this method to show a popup menu when the user right clicks a
     * node in the tree.
     *
     * @param e
     */
    @Override
    protected void showPopupMenu(final MouseEvent e) {

        // Which nodes are selected?
        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
        if (selectedTree.getTree().getSelectionPaths()!=null) {
            for(TreePath treePath : selectedTree.getTree().getSelectionPaths()) {
                nodes.add((DefaultMutableTreeNode)treePath.getLastPathComponent());
            }
        }
        
        // Create context menu
        final EntityOutlineContextMenu popupMenu = new EntityOutlineContextMenu(nodes);
        if (nodes.isEmpty()) { 
            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, getRootUniqueId(), true);
            popupMenu.addRootMenuItems();
        }
        else {
            popupMenu.addMenuItems();
        }

        popupMenu.show(selectedTree.getTree(), e.getX(), e.getY());
    }

    @Override
    protected void nodeClicked(MouseEvent e) {
        this.currUniqueId = null;
        selectNode(selectedTree.getCurrentNode());
    }

    @Override
    protected void backgroundClicked(MouseEvent e) {
        this.currUniqueId = null;
        if (!StringUtils.isEmpty(getRootUniqueId())) {
            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, getRootUniqueId(), true);
        }
    }

    @Override
    public void entityInvalidated(EntityInvalidationEvent event) {
        super.entityInvalidated(event);
        if (event.isTotalInvalidation()) {
            refresh(false, true, null);
        }
        else {
            for(Entity entity : event.getInvalidatedEntities()) {
                if (entity.getId().equals(root.getId())) {
                    try {
                        root = ModelMgr.getModelMgr().getEntityById(root.getId());
                        DefaultMutableTreeNode rootNode = selectedTree.getRootNode();
                        getEntityData(rootNode).setChildEntity(root);
                        getDynamicTree().recreateChildNodes(rootNode);
                    }
                    catch (Exception e) {
                        SessionMgr.getSessionMgr().handleException(e);
                    }
                }
            }
        }
    }
    
    @Subscribe
    public void entityCreated(EntityCreateEvent event) {
        Entity entity = event.getEntity();
        if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
            log.debug("New common root detected: {}", EntityUtils.identify(entity));

            EntityData newEd = null;
            int i = 0;
            for (EntityData ed : ModelMgrUtils.getAccessibleEntityDatasWithChildren(root)) {
                if (ed.getChildEntity().getId().equals(entity.getId())) {
                    newEd = ed;
                    break;
                }
                i++;
            }

            if (newEd == null) {
                log.error("Could not locate newly inserted common root: {}", entity.getId());
            }
            else {
                addNodes(getDynamicTree().getRootNode(), newEd, i);
            }
        }
    }

    @Subscribe
    @Override
    public void entityChanged(EntityChangeEvent event) {
        super.entityChanged(event);
        Entity entity = event.getEntity();

        Set<Entity> toRemove = new HashSet<Entity>();

        for (Entity commonRoot : ModelMgrUtils.getAccessibleChildren(root)) {
            if (commonRoot.getId().equals(entity.getId())) {
                // A common root has changed
                if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) == null) {
                    log.debug("No longer a common root: {}", EntityUtils.identify(entity));
                    // It is no longer a common root!
                    toRemove.add(commonRoot);
                }
            }
        }

        for (Entity entityRoot : toRemove) {
            log.debug("Removing entity root: {}", EntityUtils.identify(entityRoot));
            for (DefaultMutableTreeNode node : getNodesByEntityId(entityRoot.getId())) {
                if (node.getParent().equals(getTree().getModel().getRoot())) {
                    removeNode(node);
                }
            }
        }
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
    private Queue<Callable<Void>> callbacks = new ConcurrentLinkedQueue<Callable<Void>>();

    private void executeCallBacks() {
        synchronized (this) {
            for (Iterator<Callable<Void>> iterator = callbacks.iterator(); iterator.hasNext();) {
                try {
                    iterator.next().call();
                }
                catch (Exception e) {
                    log.error("Error executing callback", e);
                }
                iterator.remove();
            }
        }
    }

    public void refresh(final boolean invalidateCache, final ExpansionState expansionState, final Callable<Void> success) {

        synchronized (this) {
            if (success != null) {
                callbacks.add(success);
            }
            if (refreshInProgress.getAndSet(true)) {
                log.debug("Skipping refresh, since there is one already in progress");
                return;
            }
        }

        log.debug("Starting whole tree refresh (invalidateCache={}, restoreState={})", invalidateCache, expansionState != null);

        showLoadingIndicator();
        ModelMgr.getModelMgr().unregisterOnEventBus(EntityOutline.this);

        SimpleWorker entityOutlineLoadingWorker = new SimpleWorker() {

            private List<Entity> rootList;

            protected void doStuff() throws Exception {
                if (invalidateCache && getRootEntity() != null) {
                    ModelMgr.getModelMgr().invalidateCache();
                }
                rootList = loadRootList();
            }

            protected void hadSuccess() {
                try {
                    ModelMgr.getModelMgr().registerOnEventBus(EntityOutline.this);

                    init(rootList);
                    currUniqueId = null;
                    refreshInProgress.set(false);

                    if (expansionState != null) {
                        expansionState.restoreExpansionState(getDynamicTree(), true, new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                showTree();
                                executeCallBacks();
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
                log.error("Tree refresh encountered error", error);
                JOptionPane.showMessageDialog(EntityOutline.this, "Error loading data outline", "Data Load Error",
                        JOptionPane.ERROR_MESSAGE);
                init(null);
            }
        };

        entityOutlineLoadingWorker.execute();
    }

    public void expandByUniqueId(final String uniqueId) {
        DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
        if (node != null) {
            getDynamicTree().expand(node, true);
            return;
        }

        // Let's try to lazy load the ancestors of this node
        List<String> path = EntityUtils.getPathFromUniqueId(uniqueId);
        for (String ancestorId : path) {
            DefaultMutableTreeNode ancestor = getNodeByUniqueId(ancestorId);
            if (ancestor == null) {
                // Give up, can't find the entity with this uniqueId
                log.warn("expandByUniqueId cannot locate " + ancestorId);
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

    @Override
    public void selectEntityByUniqueId(final String uniqueId) {
        DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
        if (node != null) {
            selectNode(node);
            return;
        }

        // Let's try to lazy load the ancestors of this node
        List<String> path = EntityUtils.getPathFromUniqueId(uniqueId);
        for (String ancestorId : path) {
            DefaultMutableTreeNode ancestor = getNodeByUniqueId(ancestorId);
            if (ancestor == null) {
                // Give up, can't find the entity with this uniqueId
                log.warn("selectEntityByUniqueId cannot locate " + uniqueId);
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

    public void highlightEntityByUniqueId(final String uniqueId) {
        DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
        if (node != null) {
            getDynamicTree().navigateToNode(node);
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
                int c = JOptionPane.showOptionDialog(SessionMgr.getMainFrame(),
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
            ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, uniqueId + "", true);
        }
        else {
            return;
        }

        log.debug("Selecting node {}", uniqueId);

        DefaultMutableTreeNode node2 = getNodeByUniqueId(uniqueId);

        if (node2 == null) {
            log.warn("selectNode cannot locate " + uniqueId);
            return;
        }

        if (node != node2) {
            log.error("We have a node conflict. This should never happen! (@{} != @{})", System.identityHashCode(node), System.identityHashCode(node2));
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        if (parentNode != null && !getTree().isExpanded(new TreePath(parentNode.getPath()))) {
            getDynamicTree().expand(parentNode, true);
        }

        getDynamicTree().navigateToNode(node);

        final String finalCurrUniqueId = currUniqueId;

        if (!getDynamicTree().childrenAreLoaded(node)) {
            SessionMgr.getBrowser().getViewerManager().showLoadingIndicatorInActiveViewer();
            // Load the children before displaying them
            getDynamicTree().expandNodeWithLazyChildren(node, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    log.debug("Got lazy nodes, loading entity in viewer");
                    loadEntityInViewers(finalCurrUniqueId);
                    return null;
                }
            });
        }
        else {
            loadEntityInViewers(finalCurrUniqueId);
        }
    }

    private void loadEntityInViewers(String uniqueId) {

        log.debug("loadEntityInViewers: " + uniqueId);
        if (uniqueId == null) {
            return;
        }

        DefaultMutableTreeNode node = getNodeByUniqueId(uniqueId);
        if (node == null) {
            return;
        }

        // This method would never be called on a node whose children are lazy
        if (!getDynamicTree().childrenAreLoaded(node)) {
            throw new IllegalStateException("Cannot display entity whose children are not loaded");
        }

        RootedEntity rootedEntity = new RootedEntity(uniqueId, getEntityData(node));
        log.debug("showEntityInActiveViewer: " + rootedEntity.getName());
        SessionMgr.getBrowser().getViewerManager().showEntityInActiveViewer(rootedEntity);
    }

    @Override
    public String toString() {
        return "EntityOutline(" + root + ")";
    }
}
