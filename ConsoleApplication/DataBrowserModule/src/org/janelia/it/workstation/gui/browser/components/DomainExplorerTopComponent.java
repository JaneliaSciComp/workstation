package org.janelia.it.workstation.gui.browser.components;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.ActionMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectNodeSelectionModel;
import org.janelia.it.workstation.gui.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.gui.find.FindContext;
import org.janelia.it.workstation.gui.browser.gui.find.FindContextManager;
import org.janelia.it.workstation.gui.browser.gui.find.FindToolbar;
import org.janelia.it.workstation.gui.browser.gui.support.Debouncer;
import org.janelia.it.workstation.gui.browser.gui.support.ExpandedTreeState;
import org.janelia.it.workstation.gui.browser.gui.tree.CustomTreeToolbar;
import org.janelia.it.workstation.gui.browser.gui.tree.CustomTreeView;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNodeTracker;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.RootNode;
import org.janelia.it.workstation.gui.browser.nodes.WorkspaceNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
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
        dtd = "-//org.janelia.it.workstation.gui.browser.components//DomainExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = DomainExplorerTopComponent.TC_NAME,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "org.janelia.it.FlyWorkstation.gui.dialogs.nb.DomainExplorerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_DomainExplorerAction",
        preferredID = DomainExplorerTopComponent.TC_NAME
)
@Messages({
    "CTL_DomainExplorerAction=Domain Explorer",
    "CTL_DomainExplorerTopComponent=Domain Explorer"
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
        
        this.toolbar = new CustomTreeToolbar(beanTreeView) {
            @Override
            protected void refresh() {
                DomainExplorerTopComponent.this.refresh();
            }
        };
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

//        bindKeys();
        
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
                // Select the root node
                showRootNode();
                // Expand the top-level workspace nodes
                for(Node node : root.getChildren().getNodes()) {
                    beanTreeView.expandNode(node);
                    break; // For now, we'll only expand the user's default workspace
                }
                synchronized (DomainExplorerTopComponent.this) {
                    if (pathsToExpand!=null) {
                        beanTreeView.expand(pathsToExpand);
                        pathsToExpand = null;
                    }
                }
            }

            @Override
            protected void hadError(Throwable error) {
                // Init the list viewer manager
                DomainListViewManager.getInstance();
                showNothing();
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        
        worker.execute();
        
    }
    
//    private void bindKeys() {
//        
//        CutAction cutAction = SystemAction.get(CutAction.class);
//        PasteAction pasteAction = SystemAction.get(PasteAction.class);
//        InputMap keys = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//        KeyStroke keyCut = (KeyStroke) cutAction.getValue(Action.ACCELERATOR_KEY);
//        if (keyCut==null) {
//            keyCut = KeyStroke.getKeyStroke("D-X");
//        }
//        keys.put(keyCut, cutAction);
//        KeyStroke keyPaste = (KeyStroke) pasteAction.getValue(Action.ACCELERATOR_KEY);
//        if (keyPaste==null) {
//            keyPaste = KeyStroke.getKeyStroke("D-V");
//        }
//        keys.put(keyPaste, pasteAction);
//    }
    
    
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

    public void showTree() {
        treePanel.removeAll();
        treePanel.add(beanTreeView);
        revalidate();
        repaint();
    }
    
    private void showRootNode() {
        this.root = new RootNode();
        mgr.setRootContext(root);
        showTree();
    }
    
    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.debug("Total invalidation detected, refreshing...");
            refresh(false, true, null);
        }
        else {
            DomainModel model = DomainMgr.getDomainMgr().getModel();
            for(DomainObject domainObject : event.getDomainObjects()) {
                Set<DomainObjectNode> nodes = DomainObjectNodeTracker.getInstance().getNodesById(domainObject.getId());
                if (!nodes.isEmpty()) {
                log.info("Updating invalidated object: {}",domainObject.getName());
                    for(DomainObjectNode node : nodes) {
                        DomainObject refreshed = model.getDomainObject(domainObject.getClass(), domainObject.getId());
                        if (refreshed==null) {
                            log.info("  Destroying node@{} which is no longer relevant",System.identityHashCode(node));
                            try {
                                node.destroy();
                            }
                            catch (IOException e) {
                                log.error("  Error destroying invalidated node",e);
                            }
                        }
                        else {
                            log.info("  Updating node@{} with refreshed object",System.identityHashCode(node));
                            final List<Long[]> expanded = beanTreeView.getExpandedPaths();
                            final List<Long[]> selected = beanTreeView.getSelectedPaths();
                            node.update(refreshed);
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    beanTreeView.expand(expanded);
                                    beanTreeView.selectPaths(selected);
                                }
                            });
                        }
                    }
                }
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
        
        log.info("refresh(restoreState={})",restoreState);
        
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
                    showRootNode();
                    if (restoreState) {
                        beanTreeView.expand(expanded);
                        beanTreeView.selectPaths(selected);
                    }
                    beanTreeView.grabFocus();
                    debouncer.success();
                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                debouncer.failure();
                SessionMgr.getSessionMgr().handleException(error);
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
        final Node node = allNodes.iterator().next();
        if (node instanceof DomainObjectNode) {
            log.info("Selected node@{} -> {}",System.identityHashCode(node),node.getDisplayName());
            selectionModel.select((DomainObjectNode)node, true);
        }
    }

    public WorkspaceNode getWorkspaceNode() {
        for(Node node : root.getChildren().getNodes()) {
            return (WorkspaceNode)node;
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
    
    public RootNode getRoot() {
        return root;
    }

    public void expand(Long[] idPath) {
        beanTreeView.expand(idPath);
    }

    public Node select(Long[] idPath) {
        if (root==null) return null;
        Node node = NodeUtils.findNodeWithPath(root, idPath);
        if (node!=null) {
            log.info("Found node with path {}: {}",NodeUtils.createPathString(idPath),node.getDisplayName());
            selectNode(node);
        }
        return node;
    }
    
    public void selectNode(Node node) {
        beanTreeView.selectNode(node);
    }

    public void selectNodeById(Long id) {
        for(Node node : DomainObjectNodeTracker.getInstance().getNodesById(id)) {
            selectNode(node);
            break;
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
    public void findPrevMatch(String text, boolean skipStartingNode) {
        beanTreeView.navigateToNodeStartingWith(text, Position.Bias.Backward, skipStartingNode);
    }

    @Override
    public void findNextMatch(String text, boolean skipStartingNode) {
        beanTreeView.navigateToNodeStartingWith(text, Position.Bias.Forward, skipStartingNode);
    }
    
    @Override
    public void openMatch() {
    }
}
