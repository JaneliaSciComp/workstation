package org.janelia.it.workstation.gui.browser.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.text.DefaultEditorKit;

import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectNodeSelectionModel;
import org.janelia.it.workstation.gui.browser.nodes.CustomTreeView;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.RootNode;
import org.janelia.it.workstation.gui.browser.nodes.WorkspaceNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
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
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component for the Data Explorer, which shows an outline tree view of the
 * user's workspace.
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
    "CTL_DomainExplorerTopComponent=Domain Explorer",
    "HINT_DomainExplorerTopComponent=Browse the data"
})
public final class DomainExplorerTopComponent extends TopComponent implements ExplorerManager.Provider, LookupListener {

    public static final String TC_NAME = "DomainExplorerTopComponent";
    
    private Logger log = LoggerFactory.getLogger(DomainExplorerTopComponent.class);

    private final ExplorerManager mgr = new ExplorerManager();
    private final DomainObjectNodeSelectionModel selectionModel = new DomainObjectNodeSelectionModel();
    private Lookup.Result<AbstractNode> result = null;
    private RootNode root;
    private CustomTreeView beanTreeView;
    
    public DomainExplorerTopComponent() {
        initComponents();
        
        this.beanTreeView = new CustomTreeView(this);
        selectionModel.setSource(this);
        
        setName(Bundle.CTL_DomainExplorerTopComponent());
        setToolTipText(Bundle.HINT_DomainExplorerTopComponent());
        
        Lookup lookup = new ProxyLookup (ExplorerUtils.createLookup(mgr, getActionMap()), Lookups.singleton(selectionModel));
        associateLookup(lookup);
        
        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 

//        bindKeys();

        this.root = new RootNode();
        mgr.setRootContext(root);
        
        TreeToolbar toolbar = new TreeToolbar(root);
        add(toolbar, BorderLayout.PAGE_START);
        add(beanTreeView, BorderLayout.CENTER);
                   
        // Expand the top-level workspace nodes
        for(Node node : root.getChildren().getNodes()) {
            beanTreeView.expandNode(node);
            break; // For now, we'll only expand the user's default workspace
        }
    }
    
    public RootNode getRoot() {
        return root;
    }

    public void expand(Long[] idPath) {
        List<Long[]> paths = new ArrayList<>();
        paths.add(idPath);
        beanTreeView.expandNodes(paths);
    }

    public void select(Long[] idPath) {
        Node node = NodeUtils.findNodeWithPath(root, idPath);
        log.info("Found node with path {}: {}",NodeUtils.createPathString(idPath),node);
        selectNode(node);
    }
    
    public WorkspaceNode getWorkspaceNode() {
        for(Node node : root.getChildren().getNodes()) {
            return (WorkspaceNode)node;
        }
        return null;
    }
    
    private class TreeToolbar extends JPanel {
    
        private final JButton refreshButton;
        private final JToolBar toolBar;
        
        public TreeToolbar(final RootNode rootNode) {
            super(new BorderLayout());
        
            this.toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.setRollover(true);

            refreshButton = new JButton(Icons.getRefreshIcon());
            refreshButton.setToolTipText("Refresh the data in the tree.");
            refreshButton.setFocusable(false);
            refreshButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refresh();
                }
            });
            toolBar.add(refreshButton);

            toolBar.addSeparator();
            add(toolBar, BorderLayout.PAGE_START);
        }
        
        public JToolBar getJToolBar() {
            return toolBar;
        }
    }
    
    public static DomainExplorerTopComponent getInstance() {
        return (DomainExplorerTopComponent)WindowLocator.getByName(DomainExplorerTopComponent.TC_NAME);
    }

    public DomainObjectNodeSelectionModel getSelectionModel() {
        return selectionModel;
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
    
    public void refresh() {
        refresh(null);
    }
    
    public void refresh(final Callable<Void> success) {
                        
        final List<Long[]> expanded = beanTreeView.getExpandedPaths();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                root.refreshChildren();
            }

            @Override
            protected void hadSuccess() {
                try {
                    beanTreeView.expandNodes(expanded);
                    ConcurrentUtils.invoke(success);
                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
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
    }

    @Override
    public void componentClosed() {
        result.removeLookupListener(this);
    }
    
    @Override
    protected void componentActivated() {
        ExplorerUtils.activateActions(mgr, true);
    }
    
    @Override
    protected void componentDeactivated() {
        ExplorerUtils.activateActions(mgr, false);
    }
    
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }
    
    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        Collection<? extends AbstractNode> allNodes = result.allInstances();
        if (allNodes.isEmpty()) {
            return;
        }
        final Node node = allNodes.iterator().next();
        if (node instanceof DomainObjectNode) {
            selectionModel.select((DomainObjectNode)node, true);
        }
    }

    public void selectNode(Node node) {
        if (node==null) return;
        try {
            Node[] nodes = { node };
            mgr.setSelectedNodes(nodes);
        }
        catch (PropertyVetoException e) {
            log.error("Node selection was vetoed",e);
        }
    }

    public void selectNodeById(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
