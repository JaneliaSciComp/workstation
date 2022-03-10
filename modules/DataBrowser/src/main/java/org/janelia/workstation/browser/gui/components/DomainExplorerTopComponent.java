package org.janelia.workstation.browser.gui.components;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.find.FindContext;
import org.janelia.workstation.browser.gui.find.FindContextManager;
import org.janelia.workstation.browser.gui.find.FindToolbar;
import org.janelia.workstation.browser.gui.tree.CustomTreeToolbar;
import org.janelia.workstation.browser.gui.tree.CustomTreeView;
import org.janelia.workstation.common.gui.support.Debouncer;
import org.janelia.workstation.common.gui.support.ExpandedTreeState;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.common.nodes.AbstractDomainObjectNode;
import org.janelia.workstation.common.nodes.ExplorerRootNode;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.common.nodes.RecentlyOpenedItemsNode;
import org.janelia.workstation.common.nodes.WorkspaceNode;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.prefs.LocalPreferenceChanged;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.IdentifiableNodeSelectionModel;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.janelia.workstation.core.nodes.UserObjectNode;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.DomainObjectHandler;
import org.janelia.workstation.integration.spi.domain.ServiceAcceptorHelper;
import org.janelia.workstation.integration.spi.nodes.NodeGenerator;
import org.janelia.workstation.integration.spi.nodes.NodePreference;
import org.janelia.workstation.integration.spi.nodes.NodeProvider;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.janelia.workstation.core.options.OptionConstants.*;


/**
 * Top component for the Data Explorer, which shows an outline tree view of the
 * user's workspace.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.browser.components//DomainExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = DomainExplorerTopComponent.TC_NAME,
        iconBase = "images/folder.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "org.janelia.workstation.browser.components.DomainExplorerTopComponent")
@ActionReference(path = "Menu/Window/Core", position = 20)
@TopComponent.OpenActionRegistration(   
        displayName = "#CTL_DomainExplorerAction",
        preferredID = DomainExplorerTopComponent.TC_NAME
)
@Messages({
    "CTL_DomainExplorerAction=Data Explorer",
    "CTL_DomainExplorerTopComponent=Data Explorer"
})
public final class DomainExplorerTopComponent extends TopComponent implements ExplorerManager.Provider, FindContext {

    private static final Logger log = LoggerFactory.getLogger(DomainExplorerTopComponent.class);

    public static final String TC_NAME = "DomainExplorerTopComponent";
    public static final String TC_VERSION = "1.0";
    
    public static DomainExplorerTopComponent getInstance() {
        return (DomainExplorerTopComponent) WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);
    }

    // UI Elements
    private final CustomTreeToolbar toolbar;
    private final JPanel treePanel;
    private final CustomTreeView beanTreeView;
    private final FindToolbar findToolbar;

    // Utilities
    private final ExplorerManager mgr = new ExplorerManager();
    private final IdentifiableNodeSelectionModel selectionModel = new IdentifiableNodeSelectionModel();
    private final Debouncer debouncer = new Debouncer();

    // State
    private ExplorerRootNode root;
    private List<Long[]> pathsToExpand;
    private boolean loadInitialState = true;

    public DomainExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_DomainExplorerTopComponent());
        
        selectionModel.setSource(this);

        Lookup lookup = new ProxyLookup (ExplorerUtils.createLookup(mgr, getActionMap()), Lookups.singleton(selectionModel));
        associateLookup(lookup);
        
        this.beanTreeView = new CustomTreeView(this);
        beanTreeView.setDefaultActionAllowed(false);
        beanTreeView.setRootVisible(false);

        beanTreeView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    Node[] selectedNodes = beanTreeView.getSelectedNodes();
                    if (selectedNodes.length==1) {
                        final Node node = selectedNodes[selectedNodes.length-1];
                        navigateNode(node);
                    }
                }
            }
        });

        this.toolbar = new CustomTreeToolbar(beanTreeView) {
            @Override
            protected void refresh() {
                DomainExplorerTopComponent.this.refresh();
            }
        };

        DropDownButton configButton = new DropDownButton();
        configButton.setIcon(Icons.getIcon("cog.png"));
        configButton.setFocusable(false);
        configButton.setToolTipText("Options for the Data Explorer");

        final JCheckBoxMenuItem navigateOnClickMenuItem = new JCheckBoxMenuItem("Navigate on click", isNavigateOnClick());
        navigateOnClickMenuItem.addActionListener(e -> setNavigateOnClick(navigateOnClickMenuItem.isSelected()));
        configButton.addMenuItem(navigateOnClickMenuItem);

        // Add checkboxes for node providers

        List<NodeGenerator> allGenerators = new ArrayList<>();
        for(NodeProvider provider : Lookups.forPath(NodeProvider.LOOKUP_PATH).lookupAll(NodeProvider.class)) {
            allGenerators.addAll(provider.getNodeGenerators());
        }
        allGenerators.sort(Comparator.comparing(NodeGenerator::getIndex));

        for (NodeGenerator generator : allGenerators) {
            NodePreference nodePreference = generator.getNodePreference();
            if (nodePreference!=null) {
                final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(
                        "Show "+nodePreference.getNodeName(), nodePreference.isNodeShown());
                menuItem.addActionListener(e -> nodePreference.setNodeShown(menuItem.isSelected()));
                configButton.addMenuItem(menuItem);
            }
        }

        toolbar.getJToolBar().add(configButton);
        
        this.treePanel = new JPanel(new BorderLayout());
        this.findToolbar = new FindToolbar(this);
        
        add(toolbar, BorderLayout.PAGE_START);
        add(treePanel, BorderLayout.CENTER);
        add(findToolbar, BorderLayout.PAGE_END);

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 
        
        // Init the list viewer manager
        DomainListViewManager.getInstance();
        
        // Init the global selection model
        GlobalDomainObjectSelectionModel.getInstance();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void componentOpened() {
        Events.getInstance().registerOnEventBus(this);
        if (AccessManager.loggedIn()) {
            // This method will only run once
            loadInitialState();
        }
    }

    @Override
    public void componentClosed() {
        Events.getInstance().unregisterOnEventBus(this);
    }
    
    @Override
    protected void componentActivated() {
        log.info("Activating domain explorer");
        ExplorerUtils.activateActions(mgr, true);
        FindContextManager.getInstance().activateContext(this);
    }
    
    @Override
    protected void componentDeactivated() {
        ExplorerUtils.activateActions(mgr, false);
        FindContextManager.getInstance().deactivateContext(this);
    }

    void writeProperties(java.util.Properties p) {
        if (p==null) return;
        p.setProperty("version", TC_VERSION);
        
        List<Long[]> expandedPaths = beanTreeView.getExpandedPaths();
        ObjectMapper mapper = new ObjectMapper();
        try {
            String expandedPathStr = mapper.writeValueAsString(ExpandedTreeState.createState(expandedPaths));
            log.info("Writing state: {} expanded paths",expandedPaths.size());
            p.setProperty("expandedPaths", expandedPathStr);
        }
        catch (Exception e) {
            log.error("Error saving state",e);
        }
    }
    
    void readProperties(java.util.Properties p) {
        if (p==null) {
            log.info("No properties to read");
            return;
        }
        String version = p.getProperty("version");
        final String expandedPathStr = p.getProperty("expandedPaths");
        if (TC_VERSION.equals(version) && expandedPathStr!=null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                final ExpandedTreeState expandedState =  mapper.readValue(expandedPathStr, ExpandedTreeState.class);
                if (expandedState!=null) {
                    log.info("Reading state: {} expanded paths",expandedState.getExpandedArrayPaths().size());
                    // Must write to instance variables from EDT only
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pathsToExpand = expandedState.getExpandedArrayPaths();
                            log.info("saving pathsToExpand.size={}", pathsToExpand.size());
                            if (AccessManager.loggedIn()) {
                                loadInitialState();
                            }
                            else {
                                // Not logged in yet, wait for a SessionStartEvent
                            }
                        }
                    });
                }
            }
            catch (Exception e) {
                log.error("Error reading state",e);
            }       
        }
        else {
            log.info("Properties are out of date (version {})", version);
            
        }
    }
    
    // Custom methods
    
    public void showNothing() {
        treePanel.removeAll();
        revalidate();
        repaint();
    }

    public void showLoadingIndicator() {
        treePanel.removeAll();
        treePanel.add(new JLabel(Icons.getLoadingIcon()));
        revalidate();
        repaint();
    }

    private void showTree() {
        treePanel.removeAll();
        treePanel.add(beanTreeView);
        revalidate();
        repaint();
    }

    @Subscribe
    public void sessionStarted(SessionStartEvent event) {
        log.debug("Session started, loading initial state");
        loadInitialState();
    }
    
    private synchronized void loadInitialState() {
        
        if (!loadInitialState) return;
        log.info("Loading initial state");
        this.loadInitialState = false;
        
        showLoadingIndicator();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                // Attempt to load stuff into the cache, which the RootNode will query from the EDT
                DomainMgr.getDomainMgr().getModel().getWorkspaces();
            }

            @Override
            protected void hadSuccess() {
                // Load the data
                refresh(false, false, null);
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                FrameworkAccess.handleException(error);
            }
        };
        
        worker.execute();
    }

    private void updateRecentlyOpenedNode() {
        for(Node child : root.getChildren().getNodes()) {
            if (child instanceof RecentlyOpenedItemsNode) {
                RecentlyOpenedItemsNode node = (RecentlyOpenedItemsNode)child;
                node.refreshChildren();
            }
        }
    }

    @Subscribe
    public void prefChanged(LocalPreferenceChanged event) {
        if (event.getKey().equals(DataBrowserMgr.RECENTLY_OPENED_HISTORY)) {
            // Something was added to the history, so we need to update the node's children
            if (root!=null) {
                updateRecentlyOpenedNode();
            }
        }
        else if (event.getKey().equals(SHOW_RECENTLY_OPENED_ITEMS)
                || event.getKey().equals(SHOW_DATA_SETS)
                || event.getKey().equals(SHOW_EM_DATA_SETS)
                || event.getKey().equals(SHOW_COLOR_DEPTH_LIBRARIES)
                || event.getKey().equals(SHOW_COLOR_DEPTH_SEARCHES)
                || event.getKey().equals(SHOW_FLY_LINE_RELEASES)) {
            // Recreate the root node so that it picks up the new visibility preferences
            refresh(false, true, null); 
        }
    }

    @Subscribe
    public void objectsRemoved(DomainObjectRemoveEvent event) {

        final List<Long[]> expanded = beanTreeView.getExpandedPaths();
        DomainObject domainObject = event.getDomainObject();
        
        Set<IdentifiableNode> nodes = NodeTracker.getInstance().getNodesByObject(domainObject);
        log.info("Found {} nodes for {}",nodes.size(),domainObject);

        if (!nodes.isEmpty()) {
            boolean updated = false;
            log.info("Updating removed object: {}",domainObject.getName());
            for(IdentifiableNode node : nodes) {
                try {
                    log.info("  Destroying node@{} which is no longer relevant",System.identityHashCode(node));
                    node.destroy();
                    updated = true;
                }  
                catch (Exception ex) {
                    log.error("Error destroying node", ex);
                }
            }
            if (updated) {
                SwingUtilities.invokeLater(() -> {
                    log.info("Re-expanding {} paths after object invalidation", expanded.size());
                    beanTreeView.expand(expanded);
                });
            }
        }
    }
    
    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.debug("Total invalidation detected, refreshing...");
            refresh(false, true, null);
        }
        else {
            boolean updated = false;
            final List<Long[]> expanded = beanTreeView.getExpandedPaths();
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            for(DomainObject domainObject : event.getDomainObjects()) {
                Set<IdentifiableNode> nodes = NodeTracker.getInstance().getNodesByObject(domainObject);
                if (!nodes.isEmpty()) {
                    log.info("Updating {} nodes for invalidated object: {}", nodes.size(), domainObject.getName());
                    for(IdentifiableNode node : nodes) {
                        try {
                            DomainObject refreshed = model.getDomainObject(domainObject.getClass(), domainObject.getId());
                            if (refreshed==null) {
                                log.debug("  Destroying node@{} which is no longer relevant",System.identityHashCode(node));
                                node.destroy();
                                updated = true;
                            }
                            else if (node instanceof UserObjectNode) {
                                UserObjectNode<DomainObject> userObjectNode = ((UserObjectNode) node);
                                log.debug("  Updating node@{} with refreshed object",System.identityHashCode(node));
                                userObjectNode.update(refreshed);
                                updated = true;
                            }
                        }  
                        catch (Exception ex) {
                            log.error("Error destroying node", ex);
                        }
                    }
                }
            }

            if (updated) {
                SwingUtilities.invokeLater(() -> {
                    log.info("Re-expanding {} paths after object invalidation", expanded.size());
                    beanTreeView.expand(expanded);
                });
            }
        }
    }
    
    public void refresh() {
        refresh(true, true, null);
    }
    
    public void refresh(final Callable<Void> success) {
        refresh(true, true, success);
    }
    
    public void refresh(final boolean invalidateCache, final boolean restoreState, final Callable<Void> success) {
                        
        if (!debouncer.queue(success)) {
            log.debug("Skipping refresh, since there is one already in progress");
            return;
        }

        log.info("refresh(invalidateCache={}, restoreState={})",invalidateCache,restoreState);
        final StopWatch w = new StopWatch();
        
        final List<Long[]> expanded = root!=null && restoreState ? beanTreeView.getExpandedPaths() : null;
        final List<Long[]> selected = root!=null && restoreState ? beanTreeView.getSelectedPaths() : null;
                
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (invalidateCache) {
                    DomainMgr.getDomainMgr().getModel().invalidateAll();
                }
                // This is attempted by the RootNode below, but we try it here
                // first so that we can fail early in case there's a network 
                // problem. 
                DomainMgr.getDomainMgr().getModel().getWorkspaces();
            }

            @Override
            protected void hadSuccess() {
                try {
                    root = new ExplorerRootNode();
                    mgr.setRootContext(root);
                    showTree();

                    // This invokeLater is necessary so that the tree can be displayed before we start expanding nodes
                    SwingUtilities.invokeLater(() -> {

                        try {
                            if (pathsToExpand != null) {
                                log.info("Restoring {} expanded paths (serialized state)", pathsToExpand.size());
                                beanTreeView.expand(pathsToExpand);
                                pathsToExpand = null;
                            }
                            else {
                                if (restoreState) {
                                    if (expanded != null) {
                                    log.info("Restoring {} expanded paths", expanded.size());
                                        beanTreeView.expand(expanded);
                                    }
                                    if (selected != null) {
                                    log.info("Restoring {} selected paths", selected.size());
                                        beanTreeView.selectPaths(selected);
                                    }
                                }
                                ActivityLogHelper.logElapsed("DomainExplorer.refresh", w);
                            }
                        }
                        finally {
                            debouncer.success();
                        }
                    });

                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                debouncer.failure();
                FrameworkAccess.handleException(error);
            }
        };
        
        worker.execute();
    }

    public WorkspaceNode getWorkspaceNode() {
        if (root==null) return null;
        for(Node node : root.getChildren().getNodes()) {
            if (node instanceof WorkspaceNode) {
                return (WorkspaceNode)node;
            }
        }
        return null;
    }
    
    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }
    
    public IdentifiableNodeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void expand(Long[] idPath) {
        log.info("expand({})", idPath[idPath.length-1]);
        beanTreeView.expand(idPath);
    }

    public boolean expandNodeById(Long id) {
        log.info("expandNodeById({})", id);
        for(Node node : NodeTracker.getInstance().getNodesById(id)) {
            if (node != null) {
                Long[] idPath = NodeUtils.createIdPath(node);
                if (idPath!=null) {
                    expand(idPath);
                    return true;
                }
            }
        }
        return false;
    }

    public void selectNode(Node node) {
        log.info("selectNode({})",node.getDisplayName());
        beanTreeView.selectNode(node);
    }
    
    public Node selectNodeByPath(Long[] idPath) {
        log.info("selectAndNavigateNodeByPath({})", idPath[idPath.length-1]);
        if (root==null) return null;
        Node node = NodeUtils.findNodeWithPath(root, idPath);
        if (node!=null) {
            log.info("Found node with path {}",NodeUtils.createPathString(idPath));
            selectNode(node);
        }
        return node;
    }

    public Node selectAndNavigateNodeByPath(Long[] idPath) {
        log.info("selectAndNavigateNodeByPath({})", idPath[idPath.length-1]);
        Node selectedNode = selectNodeByPath(idPath);
        if (selectedNode!=null) {
            navigateNode(selectedNode);
        }
        return selectedNode;
    }
    
    private Node getSelectedNode(Long id) {

        Node[] selectedNodes = beanTreeView.getSelectedNodes();
        if (selectedNodes.length==1) {
            Node node = selectedNodes[0];
            if (node instanceof AbstractDomainObjectNode) {
                Long selectedId = ((AbstractDomainObjectNode<?>)node).getId();
                if (selectedId != null && selectedId.equals(id)) {
                    return node;
                }
            }
        }
        
        return null;
    }

    public Node selectNodeById(Long id) {
        log.info("selectNodeById({})", id);
        
        Node n = getSelectedNode(id);
        if (n!=null) return n;
        
        for(Node node : NodeTracker.getInstance().getNodesById(id)) {
            selectNode(node);
            return node;
        }
        return null;
    }

    public Node selectAndNavigateNodeById(Long id) {
        log.info("selectAndNavigateNodeById({})", id);

        Node selectedNode = getSelectedNode(id);
        if (selectedNode==null) {
            selectedNode = selectNodeById(id);
        }
        
        if (selectedNode!=null) {
            navigateNode(selectedNode);
        }
        
        return selectedNode;
    }

    private void navigateNode(Node node) {
        if (node instanceof AbstractDomainObjectNode) {
            log.info("Selected node@{} -> {}",System.identityHashCode(node),node.getDisplayName());
            selectionModel.select(node, true, true);
        }
    }

    @Override
    public void showFindUI() {
        findToolbar.open();
    }

    @Override
    public void hideFindUI() {
        findToolbar.close();
    }

    @Override
    public void findMatch(String text, Position.Bias bias, boolean skipStartingNode, Callable<Void> success) {
        beanTreeView.navigateToNodeStartingWith(text, bias, skipStartingNode);
        ConcurrentUtils.invokeAndHandleExceptions(success);
    }
    
    @Override
    public void openMatch() {
    }

    /**
     * Returns true if the given object can be displayed as a node in the explorer.
     * 
     * @param domainObject
     * @return
     */
    public static boolean isSupported(DomainObject domainObject) {
        DomainObjectHandler provider = ServiceAcceptorHelper.findFirstHelper(domainObject);
        return provider != null;
    }
    
    static boolean isNavigateOnClick() {
        return FrameworkAccess.getModelProperty(NAVIGATE_ON_CLICK, true);
    }
    
    private static void setNavigateOnClick(boolean value) {
        FrameworkAccess.setModelProperty(NAVIGATE_ON_CLICK, value);
    }

}
