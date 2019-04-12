package org.janelia.it.workstation.browser.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.SessionStartEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectCreateEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectInvalidationEvent;
import org.janelia.it.workstation.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.browser.events.selection.OntologySelectionEvent;
import org.janelia.it.workstation.browser.gui.dialogs.AutoAnnotationPermissionDialog;
import org.janelia.it.workstation.browser.gui.dialogs.BulkAnnotationPermissionDialog;
import org.janelia.it.workstation.browser.gui.dialogs.KeyBindDialog;
import org.janelia.it.workstation.browser.gui.find.FindContext;
import org.janelia.it.workstation.browser.gui.find.FindContextManager;
import org.janelia.it.workstation.browser.gui.find.FindToolbar;
import org.janelia.it.workstation.browser.gui.keybind.KeyBindChangedEvent;
import org.janelia.it.workstation.browser.gui.keybind.KeyBindings;
import org.janelia.it.workstation.browser.gui.keybind.KeyboardShortcut;
import org.janelia.it.workstation.browser.gui.keybind.KeymapUtil;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.ExpandedTreeState;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.JScrollPopupMenu;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.gui.tree.CustomTreeToolbar;
import org.janelia.it.workstation.browser.gui.tree.CustomTreeView;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBind;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBindings;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.actions.Action;
import org.janelia.it.workstation.browser.actions.OntologyElementAction;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.browser.nb_action.NewOntologyActionListener;
import org.janelia.it.workstation.browser.nodes.IdentifiableNode;
import org.janelia.it.workstation.browser.nodes.NodeTracker;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.OntologyNode;
import org.janelia.it.workstation.browser.nodes.OntologyRootNode;
import org.janelia.it.workstation.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.security.Subject;
import org.janelia.model.security.util.PermissionTemplate;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

/**
 * Top component for the Ontology Editor, which lets users create ontologies
 * and annotate their domain objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.browser.components//OntologyExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = OntologyExplorerTopComponent.TC_NAME,
        iconBase = "images/page.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "org.janelia.it.workstation.browser.components.OntologyExplorerTopComponent")
@ActionReference(path = "Menu/Window/Core", position = 4)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_OntologyExplorerAction",
        preferredID = OntologyExplorerTopComponent.TC_NAME
)
@Messages({
    "CTL_OntologyExplorerAction=Ontology Explorer",
    "CTL_OntologyExplorerTopComponent=Ontology Explorer"
})
public final class OntologyExplorerTopComponent extends TopComponent implements ExplorerManager.Provider, FindContext {

    private Logger log = LoggerFactory.getLogger(OntologyExplorerTopComponent.class);

    public static final String TC_NAME = "OntologyExplorerTopComponent";
    public static final String TC_VERSION = "1.0";
    
    public static OntologyExplorerTopComponent getInstance() {
        return (OntologyExplorerTopComponent) WindowLocator.getByName(OntologyExplorerTopComponent.TC_NAME);
    }

    // UI Elements
    private final CustomTreeToolbar toolbar;
    private final JPanel centerPanel;
    private final JPanel treePanel;
    private final CustomTreeView beanTreeView;
    private final FindToolbar findToolbar;
    
    // Utilities
    private final ExplorerManager mgr = new ExplorerManager();
    private final KeyListener keyListener;
    private final KeyBindDialog keyBindDialog;
    private final BulkAnnotationPermissionDialog bulkAnnotationDialog;
    private final AutoAnnotationPermissionDialog autoAnnotationDialog;
    private final Debouncer debouncer = new Debouncer();

    // State
    private final Map<OntologyTerm,OntologyElementAction> ontologyTermActionCache = new HashMap<>();
    private final List<Ontology> ontologies = new ArrayList<>();
    private OntologyRootNode root;
    private List<Long[]> pathsToExpand;
    private boolean recordingKeyBinds = false;
    private boolean loadInitialState = true;
        
    public OntologyExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_OntologyExplorerTopComponent());
        
        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));

        this.beanTreeView = new CustomTreeView(this);
        beanTreeView.setDefaultActionAllowed(false);
        beanTreeView.setRootVisible(false);
        
        beanTreeView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    Node[] selectedNodes = beanTreeView.getSelectedNodes();
                    if (selectedNodes.length==1) {
                        final Node node = selectedNodes[selectedNodes.length-1];
                        runDefaultNodeAction(node);
                    }
                }
            }
        });
        
        this.toolbar = new CustomTreeToolbar(beanTreeView) {
            @Override
            protected void refresh() {
                OntologyExplorerTopComponent.this.refresh();
            }
        };

        this.bulkAnnotationDialog = new BulkAnnotationPermissionDialog();
        this.autoAnnotationDialog = new AutoAnnotationPermissionDialog();
        
        this.treePanel = new JPanel(new BorderLayout());
        this.centerPanel = new JPanel(new BorderLayout());
        this.findToolbar = new FindToolbar(this);

        centerPanel.add(treePanel, BorderLayout.CENTER);
        centerPanel.add(findToolbar, BorderLayout.PAGE_END);
        add(toolbar, BorderLayout.PAGE_START);
        add(centerPanel, BorderLayout.CENTER);
        add(getBottomToolbar(), BorderLayout.PAGE_END);

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 
        
        // Create input listeners which will be added to the tree later
        this.keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (KeymapUtil.isModifier(e)) {
                        return;
                    }
                    
                    KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
                    Node currNode = beanTreeView.getCurrentNode();
                    
                    if (recordingKeyBinds && getOntologyNode()!=null && currNode!=null) {
                        
                        log.debug("User pressed "+e.getKeyChar());
                        e.consume();
                    
                        if (currNode instanceof OntologyTermNode) {
                            
                            log.debug("Rebinding current node: {}",currNode.getDisplayName());
                            OntologyTermNode ontologyTermNode = (OntologyTermNode) currNode;
                            OntologyElementAction action = getActionForTerm(ontologyTermNode.getOntologyTerm());
                            
                            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                                // Clear the key binding
                                KeyBindings.getKeyBindings().setBinding(null, action);
                            }
                            else {
                                // Set the key binding
                                KeyBindings.getKeyBindings().setBinding(shortcut, action);
                            }

                            // Move to the next row
                            beanTreeView.navigateToNextRow();
                            
//                            // Refresh the entire tree (another key bind may have been overridden)
//                            refresh(false, true, new Callable<Void>() {
//                                @Override
//                                public Void call() throws Exception {
//                                    // Move to the next row
//                                    beanTreeView.navigateToNextRow();
//                                    // Grab focus in preparation for the next key press
//                                    beanTreeView.grabFocus();
//                                    return null;
//                                }
//                            });
                        }
                        else {
                            // Move to the next row
                            beanTreeView.navigateToNextRow();
                        }
                    }
                    else {
                        KeyBindings.getKeyBindings().executeBinding(shortcut);
                    }
                }
            }
        };
        beanTreeView.addTreeKeyListener(keyListener);
        
        // Prepare the key binding dialog box
        this.keyBindDialog = new KeyBindDialog();
        keyBindDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                refresh();
            }
        });
    }
    
    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
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
        log.info("Activating ontology explorer");
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
                            // TODO: this currently doesn't work because we need to expandAll in order to populate the action map
//                            pathsToExpand = expandedState.getExpandedArrayPaths();
//                            log.info("saving pathsToExpand.size= "+pathsToExpand.size());
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

    public void showTree() {
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
        log.info("Loading initial session");
        this.loadInitialState = false;
        
        showLoadingIndicator();
        refresh(false, false, null);
    }
    
    @Subscribe
    public void ontologySelected(OntologySelectionEvent event) {
        Long ontologyId = event.getOntologyId();
        log.info("Refreshing because ontology '{}' was selected", ontologyId);
        refresh(false, false, null);
    }
    
    @Subscribe
    public void objectsInvalidated(DomainObjectInvalidationEvent event) {
        if (event.isTotalInvalidation()) {
            log.debug("Total invalidation detected, refreshing...");
            refresh(false, true, null);
        }
        else {
            for(DomainObject domainObject : event.getDomainObjects()) {
                if (domainObject instanceof Ontology) {
                    Ontology updatedOntology = (Ontology)domainObject;
                    // An ontology has been invalidated 
                    log.info("Refreshing because ontology '{}' has been invalidated.",updatedOntology.getName());
                    refresh(false, true, null);
                    break;
                }
            }
        }
    }

    @Subscribe
    public void objectsRemoved(DomainObjectRemoveEvent event) {
        if (event.getDomainObject() instanceof Ontology) {
            Ontology deletedOntology = (Ontology)event.getDomainObject();
            log.info("Refreshing because ontology '{}' was removed.", deletedOntology);
            refresh(false, true, null);
        }
    }
  
    @Subscribe
    public void objectCreated(DomainObjectCreateEvent event) {
        final DomainObject domainObject = event.getDomainObject();
        if (domainObject instanceof Ontology) {
            log.info("Refreshing and selecting new ontology '{}' was created.", domainObject.getName());
            StateMgr.getStateMgr().setCurrentOntologyId(domainObject.getId());
        }
    }

    @Subscribe
    public void objectChanged(DomainObjectChangeEvent event) {
        final DomainObject domainObject = event.getDomainObject();
        if (domainObject instanceof Ontology) {
            log.info("Refreshing because ontology ontology '{}' was changed.", domainObject.getName());
            refresh(false, true, null);
        }
    }
    
    public void refresh() {
        refresh(true, true, null);
    }
    
    public void refresh(final Callable<Void> success) {
        refresh(true, true, success);
    }
    
    public void refresh(final boolean invalidateCache, final boolean wantRestoreState, final Callable<Void> success) {
        
        if (!debouncer.queue(success)) {
            log.info("Skipping refresh, since there is one already in progress");
            return;
        }
        
        try {
    
            log.info("refresh(invalidateCache={},wantRestoreState={})",invalidateCache,wantRestoreState);
            final StopWatch w = new StopWatch();
    
            // Do not attempt to restore state if the ontology has changed
            Long currOntologyId = StateMgr.getStateMgr().getCurrentOntologyId();
            boolean restoreState; 
            OntologyNode ontologyNode = getOntologyNode();
            if (ontologyNode!=null && !ontologyNode.getId().equals(currOntologyId)) {
                log.info("Skipping restoreState because ontology has changed");
                restoreState = false;
                pathsToExpand = null;
            }
            else {
                restoreState = wantRestoreState;
            }
            
            final List<Long[]> expanded = root!=null && restoreState ? beanTreeView.getExpandedPaths() : null;
            final List<Long[]> selected = root!=null && restoreState ? beanTreeView.getSelectedPaths() : null;
    
            SimpleWorker worker = new SimpleWorker() {
    
                @Override
                protected void doStuff() throws Exception {
                    if (invalidateCache) {
                        DomainMgr.getDomainMgr().getModel().invalidateAll();
                    }
                    Ontology currOntology = loadOntologies();
                    loadOntologyKeybinds(currOntology);
                }
    
                @Override
                protected void hadSuccess() {
                    try {
                        root = new OntologyRootNode();
                        mgr.setRootContext(root);
                        showTree();
                        
                        if (pathsToExpand!=null) {
                            log.info("Restoring serialized expanded state");
                            beanTreeView.expand(pathsToExpand);
                            pathsToExpand = null;
                        }
                        else {
                            if (restoreState) {
                                log.info("Restoring expanded state");
                                if (expanded!=null) {
                                    beanTreeView.expand(expanded);
                                }
                                if (selected!=null) {
                                    beanTreeView.selectPaths(selected);
                                }
                            }
                            else {
                                // Expand all nodes by default
                                SwingUtilities.invokeLater(() -> {
                                    // expandAll has to happen after tree is fully rendered
                                    log.info("Expanding all nodes");
                                    beanTreeView.expandAll();
                                });
                            }
                        }
    
                        ActivityLogHelper.logElapsed("OntologyExplorerTopComponent.refresh", w);
                        debouncer.success();
                    }
                    catch (Exception e) {
                        hadError(e);
                    }
                }
    
                @Override
                protected void hadError(Throwable error) {
                    debouncer.failure();
                    FrameworkImplProvider.handleException(error);
                }
            };
            
            worker.execute();
        
        }
        catch (Exception e) {
            debouncer.failure();
            throw e;
        }
    }

    public OntologyNode getOntologyNode() {
        if (root==null) return null;
        for(Node node : root.getChildren().getNodes()) {
            if (node instanceof OntologyNode) {
                return (OntologyNode)node;
            }
        }
        return null;
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }

    /**
     * Reloads all ontologies and returns the currently selected one. 
     * @return
     * @throws Exception
     */
    private Ontology loadOntologies() throws Exception {
        ontologies.clear();
        ontologies.addAll(DomainMgr.getDomainMgr().getModel().getOntologies());  
        
        Long currOntologyId = StateMgr.getStateMgr().getCurrentOntologyId();
        for(Ontology ontology: ontologies) {
            if (ontology.getId().equals(currOntologyId)) {
                return ontology;
            }
        }
        
        return null;
    }
    
    public OntologyTermNode select(Long[] idPath) {
        OntologyNode ontologyNode = getOntologyNode();
        if (ontologyNode==null) {
            log.info("No ontology is selected");
            return null;
        }
        String pathStr = NodeUtils.createPathString(idPath);
        Node node = NodeUtils.findNodeWithPath(root, idPath);
        if (node!=null) {
            log.info("Found node with path {}: {}",pathStr,node.getDisplayName());
            beanTreeView.selectNode(node);
        }
        return (OntologyTermNode)node;
    }
    
    public KeyBindDialog getKeyBindDialog() {
        return keyBindDialog;
    }
    
    private JToolBar getBottomToolbar() {

        final Component mainFrame = FrameworkImplProvider.getMainFrame();
    
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        
        if (ontologies != null) {
            final JButton ontologyButton = new JButton("Open ontology...");
            ontologyButton.setIcon(Icons.getIcon("open_action.png"));
            ontologyButton.setToolTipText("Open ontology");
            ontologyButton.setFocusable(false);
            ontologyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    final JScrollPopupMenu ontologyListMenu = new JScrollPopupMenu();
                    ontologyListMenu.setMaximumVisibleRows(50);

                    Long currOntologyId = StateMgr.getStateMgr().getCurrentOntologyId();
                    for (final Ontology ontology : ontologies) {
                        Subject subject = null;
                        try {
                            // TODO: this should happen in a background thread
                            subject = AccessManager.getSubjectByNameOrKey(ontology.getOwnerKey());
                        }
                        catch (Exception ex) {
                            log.error("Error getting subject: "+ontology.getOwnerKey(),ex);
                        }
                        String owner = subject==null?ontology.getOwnerKey():subject.getFullName();
                        boolean checked = currOntologyId != null && ontology.getId().equals(currOntologyId);
                        JMenuItem roleMenuItem = new JRadioButtonMenuItem(ontology.getName() + " (" + owner + ")", checked);
                        String iconName = ClientDomainUtils.isOwner(ontology)?"folder.png":"folder_blue.png";
                        roleMenuItem.setIcon(Icons.getIcon(iconName));
                        roleMenuItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                ActivityLogHelper.logUserAction("OntologyExplorerTopComponent.openOntology", ontology);
                                StateMgr.getStateMgr().setCurrentOntologyId(ontology.getId());
                            }
                        });
                        ontologyListMenu.add(roleMenuItem);
                    }

                    ontologyListMenu.add(new JSeparator());

                    JMenuItem addMenuItem = new JMenuItem("Create New Ontology...");
                    addMenuItem.setIcon(Icons.getIcon("page_add.png"));
                    addMenuItem.addActionListener(new NewOntologyActionListener());
                    ontologyListMenu.add(addMenuItem);

                    ontologyListMenu.show(ontologyButton, 0, ontologyButton.getHeight());
                }
            });
            ontologyButton.addMouseListener(new MouseForwarder(toolBar, "OntologyButton->JToolBar"));
            toolBar.add(ontologyButton);
        }

        final JToggleButton keyBindButton = new JToggleButton();
        keyBindButton.setIcon(Icons.getIcon("keyboard_add.png"));
        keyBindButton.setToolTipText("Enter key binding mode");
        keyBindButton.setFocusable(false);
        keyBindButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                OntologyNode ontologyNode = getOntologyNode();
                if (ontologyNode!=null) {
                    if (keyBindButton.isSelected()) {
                        keyBindButton.setToolTipText("Exit key binding mode");
                        recordingKeyBinds = true;
                        // Transfer focus to a node in the tree in preparation for key presses
                        beanTreeView.grabFocus();
                        ActivityLogHelper.logUserAction("OntologyExplorerTopComponent.enterKeyBindingMode");
                    }
                    else {
                        keyBindButton.setToolTipText("Enter key binding mode");
                        recordingKeyBinds = false;
                        saveOntologyKeybinds(ontologyNode.getOntology());
                        ActivityLogHelper.logUserAction("OntologyExplorerTopComponent.exitKeyBindingMode");
                    }
                }
            }
        });
        toolBar.add(keyBindButton);

        final JToggleButton autoShareButton = new JToggleButton();
        autoShareButton.setIcon(Icons.getIcon("group_gear.png"));
        autoShareButton.setToolTipText("Configure annotation auto-sharing");
        autoShareButton.setFocusable(false);
        autoShareButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoShareButton.isSelected()) {
                    boolean pressedOk = autoAnnotationDialog.showAutoAnnotationConfiguration();
                    if (pressedOk) {
                        PermissionTemplate template = StateMgr.getStateMgr().getAutoShareTemplate();
                        if (template!=null) {
                            JOptionPane.showMessageDialog(mainFrame,
                                "Auto-sharing annotation with "+
                               DomainUtils.getNameFromSubjectKey(template.getSubjectKey()),
                                "Auto-sharing ended", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
                else {
                    StateMgr.getStateMgr().setAutoShareTemplate(null);
                    JOptionPane.showMessageDialog(mainFrame,
                        "No longer auto-sharing annotations", "Auto-sharing ended", JOptionPane.INFORMATION_MESSAGE);
                }

                autoShareButton.setSelected(StateMgr.getStateMgr().getAutoShareTemplate()!=null);
            }

        });
        autoShareButton.setSelected(StateMgr.getStateMgr().getAutoShareTemplate()!=null);
        toolBar.add(autoShareButton);
                    
        final JButton bulkPermissionsButton = new JButton();
        bulkPermissionsButton.setIcon(Icons.getIcon("group_edit.png"));
        bulkPermissionsButton.setToolTipText("Bulk-edit permissions for annotations on selected entities");
        bulkPermissionsButton.setFocusable(false);
        bulkPermissionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bulkAnnotationDialog.showForSelectedDomainObjects();
            }
            
        });
        toolBar.add(bulkPermissionsButton);
        
        return toolBar;
    }

    private void runDefaultNodeAction(Node node) {
        if (node instanceof OntologyTermNode) {
            log.info("Running action for node@{} -> {}",System.identityHashCode(node),node.getDisplayName());
            OntologyTermNode termNode = (OntologyTermNode)node;
            ApplyAnnotationAction.get().performAction(termNode.getOntologyTerm());
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
    
    public void showKeyBindDialog(OntologyTermNode node) {
        OntologyElementAction action = getActionForTerm(node.getOntologyTerm());
        if (action!=null) {
            getKeyBindDialog().showForAction(action);
        }
    }

    public void executeBinding(Long ontologyTermId) {

        for (IdentifiableNode<?> identifiableNode : NodeTracker.getInstance().getNodesById(ontologyTermId)) {

            // Now we can select the node we actually want
            Long[] path = NodeUtils.createIdPath(identifiableNode);
            OntologyTermNode node = select(path);
            if (node!=null) {
                javax.swing.Action a = node.getPreferredAction();
                if (a != null) {
                    log.info("Executing default node action for: {}",node.getOntologyTerm());
                    a.actionPerformed(new ActionEvent(node, ActionEvent.ACTION_PERFORMED, ""));
                }
                
                // Just execute once, even if there are multiple nodes with the same id handing around
                break;
            }   
        }
    }

    public OntologyElementAction getActionForTerm(OntologyTerm ontologyTerm) {
        OntologyElementAction action = ontologyTermActionCache.get(ontologyTerm);
        if (action == null) {
            action = new OntologyElementAction(ontologyTerm);
            ontologyTermActionCache.put(ontologyTerm, action);
        }
        return action;
    }
    
    @Subscribe
    public void keybindChanged(KeyBindChangedEvent event) {
        if (event.getNewAction() instanceof OntologyElementAction) {
            OntologyElementAction action = (OntologyElementAction)event.getNewAction();
            
            OntologyNode ontologyNode = getOntologyNode();
            if (ontologyNode!=null) {
                saveOntologyKeybinds(ontologyNode.getOntology());
            }

            Action existingAction = event.getExistingAction();
            if (existingAction!=null) {
                // Refresh the nodes that are losing the keybind
                if (existingAction instanceof OntologyElementAction) {
                    OntologyElementAction existingAction2 = (OntologyElementAction)existingAction;
                    for (IdentifiableNode<?> node : NodeTracker.getInstance().getNodesById(existingAction2.getOntologyTermId())) {
                        if (node instanceof OntologyTermNode) {
                            ((OntologyTermNode)node).fireShortcutChanged();
                        }
                    }
                }
            }
            
            // Refresh the nodes gaining the keybind
            for (IdentifiableNode<?> node : NodeTracker.getInstance().getNodesById(action.getOntologyTermId())) {
                if (node instanceof OntologyTermNode) {
                    OntologyTermNode ontologyTermNode = (OntologyTermNode)node;
                    ontologyTermNode.fireShortcutChanged();
                }
            }
        }
    }
    
    /**
     * Load the key binding preferences for a given ontology.
     */
    public void loadOntologyKeybinds(Ontology ontology) {

        if (ontology == null) return;
        Long rootId = ontology.getId();
        log.info("Loading key bindings for ontology {}", rootId);
        ontologyTermActionCache.clear();
        
        try {
            OntologyKeyBindings ontologyKeyBindings = StateMgr.getStateMgr().loadOntologyKeyBindings(rootId);
            if (ontologyKeyBindings!=null) {
                Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();

                for (OntologyKeyBind bind : keybinds) {
                    KeyboardShortcut shortcut = KeyboardShortcut.fromString(bind.getKey());

                    try {
                        OntologyTerm ontologyTerm = ontology.findTerm(bind.getOntologyTermId());
                        if (ontologyTerm!=null) {
                            OntologyElementAction action = getActionForTerm(ontologyTerm);
                            log.debug("  Loading binding {} = {}", shortcut, action.getOntologyTerm().getName());
                            KeyBindings.getKeyBindings().setBinding(shortcut, action, false);
                        }
                    }
                    catch (Exception e) {
                        log.error("Could not load key binding from user preference '" + bind.getKey() + "'.", e);
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Could not load user's key binding preferences", e);
            FrameworkImplProvider.handleException(e);
        }
    }

    /**
     * Save the key binding preferences for a given ontology.
     */
    public void saveOntologyKeybinds(Ontology ontology) {

        if (ontology == null) return;
        Long rootId = ontology.getId();
        log.info("Saving key bindings for ontology {}", rootId);

        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(AccessManager.getSubjectKey(), rootId);
        try {
            Map<KeyboardShortcut, Action> bindings = KeyBindings.getKeyBindings().getBindings();
            for (Map.Entry<KeyboardShortcut, Action> entry : bindings.entrySet()) {
                if (entry.getValue() instanceof OntologyElementAction) {
                    KeyboardShortcut shortcut = entry.getKey();
                    OntologyElementAction action = (OntologyElementAction) entry.getValue();
                    log.debug("  Saving binding {} = {}", shortcut, action.getOntologyTerm().getName());
                    ontologyKeyBindings.addBinding(shortcut.toString(), action.getOntologyTermId());
                }
            }

            StateMgr.getStateMgr().saveOntologyKeyBindings(ontologyKeyBindings);
        }
        catch (Exception e) {
            log.error("Could not save user's key binding preferences", e);
            FrameworkImplProvider.handleException(e);
        }
    }
}
