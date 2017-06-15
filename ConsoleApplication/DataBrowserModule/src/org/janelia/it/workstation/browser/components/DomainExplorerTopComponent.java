package org.janelia.it.workstation.browser.components;

import static org.janelia.it.workstation.browser.gui.options.OptionConstants.NAVIGATE_ON_CLICK;
import static org.janelia.it.workstation.browser.gui.options.OptionConstants.SHOW_RECENTLY_OPENED_ITEMS;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.ActionMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;

import org.janelia.it.jacs.integration.framework.domain.DomainObjectHelper;
import org.janelia.it.jacs.integration.framework.domain.ServiceAcceptorHelper;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.events.prefs.LocalPreferenceChanged;
import org.janelia.it.workstation.browser.events.selection.DomainObjectNodeSelectionModel;
import org.janelia.it.workstation.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.browser.gui.find.FindContext;
import org.janelia.it.workstation.browser.gui.find.FindContextManager;
import org.janelia.it.workstation.browser.gui.find.FindToolbar;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.ScrollingDropDownButton;
import org.janelia.it.workstation.browser.gui.support.ExpandedTreeState;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.gui.tree.CustomTreeToolbar;
import org.janelia.it.workstation.browser.gui.tree.CustomTreeView;
import org.janelia.it.workstation.browser.nodes.AbstractDomainObjectNode;
import org.janelia.it.workstation.browser.nodes.DomainObjectNodeTracker;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.RootNode;
import org.janelia.it.workstation.browser.nodes.WorkspaceNode;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;


/**
 * Top component for the Data Explorer, which shows an outline tree view of the
 * user's workspace.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.browser.components//DomainExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = DomainExplorerTopComponent.TC_NAME,
        iconBase = "images/folder.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "org.janelia.it.workstation.browser.components.DomainExplorerTopComponent")
@ActionReference(path = "Menu/Window/Core", position = 1)
@TopComponent.OpenActionRegistration(   
        displayName = "#CTL_DomainExplorerAction",
        preferredID = DomainExplorerTopComponent.TC_NAME
)
@Messages({
    "CTL_DomainExplorerAction=Data Explorer",
    "CTL_DomainExplorerTopComponent=Data Explorer"
})
public final class DomainExplorerTopComponent extends TopComponent implements ExplorerManager.Provider, LookupListener, FindContext {

    private static final Logger log = LoggerFactory.getLogger(DomainExplorerTopComponent.class);

    public static final String TC_NAME = "DomainExplorerTopComponent";
    public static final String TC_VERSION = "1.0";
    
    public static DomainExplorerTopComponent getInstance() {
        return (DomainExplorerTopComponent)WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);
    }

    private final CustomTreeToolbar toolbar;
    private final JPanel treePanel;
    private final CustomTreeView beanTreeView;
    private final FindToolbar findToolbar;
    
    private final ExplorerManager mgr = new ExplorerManager();
    private final DomainObjectNodeSelectionModel selectionModel = new DomainObjectNodeSelectionModel();
    private final Debouncer debouncer = new Debouncer();

    private Lookup.Result<AbstractNode> result = null;
    private RootNode root;
    private List<Long[]> pathsToExpand;

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
                    if (selectedNodes.length>0) {
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

        ScrollingDropDownButton configButton = new ScrollingDropDownButton();
        
        configButton.setIcon(Icons.getIcon("cog.png"));
        configButton.setFocusable(false);
        configButton.setToolTipText("Options for the Data Explorer");

        final JCheckBoxMenuItem navigateOnClickMenuItem = new JCheckBoxMenuItem("Navigate on click", isNavigateOnClick());
        navigateOnClickMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setNavigateOnClick(navigateOnClickMenuItem.isSelected());   
            }
        });
        configButton.getPopupMenu().add(navigateOnClickMenuItem);

        final JCheckBoxMenuItem showRecentItemsMenuItem = new JCheckBoxMenuItem("Show recently opened items", isShowRecentMenuItems());
        showRecentItemsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setShowRecentMenuItems(showRecentItemsMenuItem.isSelected());
            }
        });
        configButton.getPopupMenu().add(showRecentItemsMenuItem);
        
        toolbar.getJToolBar().add(configButton);
        
        this.treePanel = new JPanel(new BorderLayout());
        this.findToolbar = new FindToolbar(this);
        
        add(toolbar, BorderLayout.PAGE_START);
        add(treePanel, BorderLayout.CENTER);
        add(findToolbar, BorderLayout.PAGE_END);

        showLoadingIndicator();

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                // Attempt to load stuff into the cache, which the RootNode will query from the EDT
                DomainMgr.getDomainMgr().getModel().getWorkspaces();
            }

            @Override
            protected void hadSuccess() {
                // Init the list viewer manager
                DomainListViewManager.getInstance();
                // Init the global selection model
                GlobalDomainObjectSelectionModel.getInstance();
                // Load the data
                refresh(false, false, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if (pathsToExpand!=null) {
                            log.info("Restoring serialized expanded state");
                            beanTreeView.expand(pathsToExpand);
                            pathsToExpand = null;
                        }
                        return null;
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                // Init the list viewer manager
                DomainListViewManager.getInstance();
                showNothing();
                ConsoleApp.handleException(error);
            }
        };
        
        worker.execute();
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
        result = getLookup().lookupResult(AbstractNode.class);
        result.addLookupListener(this);
        Events.getInstance().registerOnEventBus(this);
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
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
        String version = p.getProperty("version");
        final String expandedPathStr = p.getProperty("expandedPaths");
        if (TC_VERSION.equals(version) && expandedPathStr!=null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                final ExpandedTreeState expandedState =  mapper.readValue(expandedPathStr, ExpandedTreeState.class);
                if (expandedState!=null) {
                    log.info("Reading state: {} expanded paths",expandedState.getExpandedArrayPaths().size());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pathsToExpand = expandedState.getExpandedArrayPaths();
                        }
                    });
                }
            }
            catch (Exception e) {
                log.error("Error reading state",e);
            }       
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
    public void prefChanged(LocalPreferenceChanged event) {
        if (event.getKey().equals(StateMgr.RECENTLY_OPENED_HISTORY)) {
            // Something was added to the history, so we need to update the node's children
            if (root!=null) {
                root.getRecentlyOpenedItemsNode().refreshChildren();
            }
            
        }
        else if (event.getKey().equals(SHOW_RECENTLY_OPENED_ITEMS)) {
            // Recreate the root node so that it picks up the new preferences
            refresh(false, true, null); 
        }
    }

    @Subscribe
    public void objectsInvalidated(DomainObjectRemoveEvent event) {

        final List<Long[]> expanded = beanTreeView.getExpandedPaths();
        DomainObject domainObject = event.getDomainObject();
        
        Set<AbstractDomainObjectNode<DomainObject>> nodes = DomainObjectNodeTracker.getInstance().getNodesByDomainObject(domainObject);
        if (!nodes.isEmpty()) {
            log.info("Updating removed object: {}",domainObject.getName());
            for(AbstractDomainObjectNode<DomainObject> node : nodes) {
                try {
                    log.info("  Destroying node@{} which is no longer relevant",System.identityHashCode(node));
                    node.destroy();
                }  
                catch (Exception ex) {
                    log.error("Error destroying node", ex);
                }
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                beanTreeView.expand(expanded);
            }
        });
    }
    
    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.debug("Total invalidation detected, refreshing...");
            refresh(false, true, null);
        }
        else {
            final List<Long[]> expanded = beanTreeView.getExpandedPaths();
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            for(DomainObject domainObject : event.getDomainObjects()) {
                Set<AbstractDomainObjectNode<DomainObject>> nodes = DomainObjectNodeTracker.getInstance().getNodesByDomainObject(domainObject);
                if (!nodes.isEmpty()) {
                    log.info("Updating invalidated object: {}",domainObject.getName());
                    for(AbstractDomainObjectNode<DomainObject> node : nodes) {
                        try {
                            DomainObject refreshed = model.getDomainObject(domainObject.getClass(), domainObject.getId());
                            if (refreshed==null) {
                                log.info("  Destroying node@{} which is no longer relevant",System.identityHashCode(node));
                                node.destroy();
                            }
                            else {
                                log.info("  Updating node@{} with refreshed object",System.identityHashCode(node));
                                node.update(refreshed);
                            }
                        }  
                        catch (Exception ex) {
                            log.error("Error destroying node", ex);
                        }
                    }
                }
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    beanTreeView.expand(expanded);
                }
            });
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

        log.info("refresh(restoreState={})",restoreState);
        final StopWatch w = new StopWatch();
        
        final List<Long[]> expanded = root!=null && restoreState ? beanTreeView.getExpandedPaths() : null;
        final List<Long[]> selected = root!=null && restoreState ? beanTreeView.getSelectedPaths() : null;
                
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (invalidateCache) {
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    model.invalidateAll();
                }
            }

            @Override
            protected void hadSuccess() {
                try {
                    root = new RootNode();
                    mgr.setRootContext(root);
                    showTree();
                    
                    int numExpanded = 0;
                    if (restoreState) {
                        numExpanded = beanTreeView.expand(expanded);
                        beanTreeView.selectPaths(selected);
                    }
                    if (numExpanded==0) {
                        log.info("Expanding first node");
                        for(Node node : root.getChildren().getNodes()) {
                            beanTreeView.expandNode(node);
                            if (node instanceof WorkspaceNode) {
                                break; // Expand everything up to and including the first Home
                            }
                        }
                    }

                    ActivityLogHelper.logElapsed("DomainExplorer.refresh", w);
                    debouncer.success();
                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                debouncer.failure();
                ConsoleApp.handleException(error);
            }
        };
        
        worker.execute();
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends AbstractNode> allNodes = result.allInstances();
        if (allNodes.isEmpty()) {
            return;
        }
        final Node selectedNode = allNodes.iterator().next();
        ActivityLogHelper.logUserAction("DomainExplorerTopComponent.resultChanged", selectedNode.getDisplayName());
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
    
    public DomainObjectNodeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void expand(Long[] idPath) {
        beanTreeView.expand(idPath);
    }

    public void expandNodeById(Long id) {
        for(Node node : DomainObjectNodeTracker.getInstance().getNodesById(id)) {
            expand(NodeUtils.createIdPath(node));
            break;
        }
    }

    public void selectNode(Node node) {
        log.info("selectNode({})",node.getDisplayName());
        beanTreeView.selectNode(node);
    }
    
    public Node selectNodeByPath(Long[] idPath) {
        if (root==null) return null;
        Node node = NodeUtils.findNodeWithPath(root, idPath);
        if (node!=null) {
            log.info("Found node with path {}",NodeUtils.createPathString(idPath));
            selectNode(node);
        }
        return node;
    }

    public Node selectAndNavigateNodeByPath(Long[] idPath) {
        Node selectedNode = selectNodeByPath(idPath);
        if (selectedNode!=null) {
            navigateNode(selectedNode);
        }
        return selectedNode;
    }

    public Node selectNodeById(Long id) {
        for(Node node : DomainObjectNodeTracker.getInstance().getNodesById(id)) {
            selectNode(node);
            return node;
        }
        return null;
    }

    public Node selectAndNavigateNodeById(Long id) {
        Node selectedNode = selectNodeById(id);
        if (selectedNode!=null) {
            navigateNode(selectedNode);
        }
        return selectedNode;
    }

    private void navigateNode(Node node) {
        if (node instanceof AbstractDomainObjectNode) {
            log.info("Selected node@{} -> {}",System.identityHashCode(node),node.getDisplayName());
            selectionModel.select((AbstractDomainObjectNode<?>)node, true, true);
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
        DomainObjectHelper provider = ServiceAcceptorHelper.findFirstHelper(domainObject);
        if (provider!=null) {
            return true;
        }
        return false;
    }
    
    public static boolean isNavigateOnClick() {
        Boolean navigate = (Boolean) ConsoleApp.getConsoleApp().getModelProperty(NAVIGATE_ON_CLICK);
        return navigate==null || navigate;
    }
    
    private static void setNavigateOnClick(boolean value) {
        ConsoleApp.getConsoleApp().setModelProperty(NAVIGATE_ON_CLICK, value);  
    }

    public static boolean isShowRecentMenuItems() {
        Boolean navigate = (Boolean) ConsoleApp.getConsoleApp().getModelProperty(SHOW_RECENTLY_OPENED_ITEMS);
        return navigate==null || navigate;
    }
    
    private static void setShowRecentMenuItems(boolean value) {
        ConsoleApp.getConsoleApp().setModelProperty(SHOW_RECENTLY_OPENED_ITEMS, value);  
    }
}
