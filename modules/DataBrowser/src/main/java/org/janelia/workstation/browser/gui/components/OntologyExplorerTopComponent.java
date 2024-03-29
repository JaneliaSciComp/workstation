package org.janelia.workstation.browser.gui.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.security.Subject;
import org.janelia.model.security.util.PermissionTemplate;
import org.janelia.workstation.browser.actions.NewOntologyActionListener;
import org.janelia.workstation.browser.actions.OntologyElementAction;
import org.janelia.workstation.browser.actions.context.ApplyAnnotationActionListener;
import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.browser.gui.dialogs.AutoAnnotationPermissionDialog;
import org.janelia.workstation.browser.gui.dialogs.BulkChangePermissionDialog;
import org.janelia.workstation.browser.gui.dialogs.KeyBindDialog;
import org.janelia.workstation.browser.gui.find.FindContext;
import org.janelia.workstation.browser.gui.find.FindContextManager;
import org.janelia.workstation.browser.gui.find.FindToolbar;
import org.janelia.workstation.browser.gui.tree.CustomTreeToolbar;
import org.janelia.workstation.browser.gui.tree.CustomTreeView;
import org.janelia.workstation.browser.nodes.OntologyNode;
import org.janelia.workstation.browser.nodes.OntologyRootNode;
import org.janelia.workstation.browser.nodes.OntologyTermNode;
import org.janelia.workstation.common.gui.support.*;
import org.janelia.workstation.common.nodes.NodeUtils;
import org.janelia.workstation.core.actions.Action;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.DomainObjectChangeEvent;
import org.janelia.workstation.core.events.model.DomainObjectCreateEvent;
import org.janelia.workstation.core.events.model.DomainObjectInvalidationEvent;
import org.janelia.workstation.core.events.model.DomainObjectRemoveEvent;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.events.selection.OntologySelectionEvent;
import org.janelia.workstation.core.keybind.KeyBindChangedEvent;
import org.janelia.workstation.core.keybind.KeyBindings;
import org.janelia.workstation.core.keybind.KeyboardShortcut;
import org.janelia.workstation.core.keybind.KeymapUtil;
import org.janelia.workstation.core.model.keybind.OntologyKeyBind;
import org.janelia.workstation.core.model.keybind.OntologyKeyBindings;
import org.janelia.workstation.core.nodes.IdentifiableNode;
import org.janelia.workstation.core.nodes.NodeTracker;
import org.janelia.workstation.core.util.ConcurrentUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
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

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Top component for the Ontology Editor, which lets users create ontologies
 * and annotate their domain objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.browser.components//OntologyExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = OntologyExplorerTopComponent.TC_NAME,
        iconBase = "images/page.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false, position = 500)
@ActionID(category = "Window", id = "org.janelia.workstation.browser.components.OntologyExplorerTopComponent")
@ActionReference(path = "Menu/Window/Core", position = 40)
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
    private final BulkChangePermissionDialog bulkAnnotationDialog;
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

        this.bulkAnnotationDialog = new BulkChangePermissionDialog();
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
        log.info("Loading initial state");
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
            refresh(false, false, null);
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

                        // This invokeLater is necessary so that the tree can be displayed before we start expanding nodes
                        SwingUtilities.invokeLater(() -> {

                            try {
                                if (pathsToExpand != null) {
                                    log.info("Restoring serialized expanded state");
                                    beanTreeView.expand(pathsToExpand);
                                    pathsToExpand = null;
                                }
                                else {
                                    if (restoreState) {
                                        log.info("Restoring expanded state");
                                        if (expanded != null) {
                                            beanTreeView.expand(expanded);
                                        }
                                        if (selected != null) {
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

        final Component mainFrame = FrameworkAccess.getMainFrame();
    
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        final JButton ontologyButton = new JButton("Open ontology...");
        ontologyButton.setIcon(Icons.getIcon("open_action.png"));
        ontologyButton.setToolTipText("Open ontology");
        ontologyButton.setFocusable(false);
        ontologyButton.addActionListener(e -> {

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
                roleMenuItem.addActionListener(e1 -> {
                    ActivityLogHelper.logUserAction("OntologyExplorerTopComponent.openOntology", ontology);
                    StateMgr.getStateMgr().setCurrentOntologyId(ontology.getId());
                });
                ontologyListMenu.add(roleMenuItem);
            }

            ontologyListMenu.add(new JSeparator());

            JMenuItem addMenuItem = new JMenuItem("Create New Ontology...");
            addMenuItem.setIcon(Icons.getIcon("page_add.png"));
            addMenuItem.addActionListener(new NewOntologyActionListener());
            ontologyListMenu.add(addMenuItem);

            ontologyListMenu.show(ontologyButton, 0, ontologyButton.getHeight());
        });
        ontologyButton.addMouseListener(new MouseForwarder(toolBar, "OntologyButton->JToolBar"));
        toolBar.add(ontologyButton);

        final JToggleButton keyBindButton = new JToggleButton();
        keyBindButton.setIcon(Icons.getIcon("keyboard_add.png"));
        keyBindButton.setToolTipText("Enter key binding mode");
        keyBindButton.setFocusable(false);
        keyBindButton.addActionListener(actionEvent -> {
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
        });
        toolBar.add(keyBindButton);

        final JToggleButton autoShareButton = new JToggleButton();
        autoShareButton.setIcon(Icons.getIcon("group_gear.png"));
        autoShareButton.setToolTipText("Configure annotation auto-sharing");
        autoShareButton.setFocusable(false);
        autoShareButton.addActionListener(e -> {
            if (autoShareButton.isSelected()) {
                boolean pressedOk = autoAnnotationDialog.showAutoAnnotationConfiguration();
                if (pressedOk) {
                    PermissionTemplate template = DataBrowserMgr.getDataBrowserMgr().getAutoShareTemplate();
                    if (template!=null) {
                        JOptionPane.showMessageDialog(mainFrame,
                            "Auto-sharing annotation with "+
                           DomainUtils.getNameFromSubjectKey(template.getSubjectKey()),
                            "Auto-sharing ended", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
            else {
                DataBrowserMgr.getDataBrowserMgr().setAutoShareTemplate(null);
                JOptionPane.showMessageDialog(mainFrame,
                    "No longer auto-sharing annotations", "Auto-sharing ended", JOptionPane.INFORMATION_MESSAGE);
            }

            autoShareButton.setSelected(DataBrowserMgr.getDataBrowserMgr().getAutoShareTemplate()!=null);
        });
        autoShareButton.setSelected(DataBrowserMgr.getDataBrowserMgr().getAutoShareTemplate()!=null);
        toolBar.add(autoShareButton);
                    
        final JButton bulkPermissionsButton = new JButton();
        bulkPermissionsButton.setIcon(Icons.getIcon("group_edit.png"));
        bulkPermissionsButton.setToolTipText("Bulk-edit permissions for annotations on selected entities");
        bulkPermissionsButton.setFocusable(false);
        bulkPermissionsButton.addActionListener(e -> {

            DomainListViewTopComponent listView = DomainListViewManager.getInstance().getActiveViewer();
            if (listView==null || listView.getEditor()==null) {
                showSelectionMessage();
                return;
            }

            ChildSelectionModel<?,?> selectionModel = listView.getEditor().getSelectionModel();
            if (selectionModel==null) {
                showSelectionMessage();
                return;
            }

            List<Reference> selected = new ArrayList<>();
            for(Object id : selectionModel.getSelectedIds()) {
                if (id instanceof Reference) {
                    selected.add((Reference)id);
                }
            }

            if (selected.isEmpty()) {
                showSelectionMessage();
                return;
            }

            bulkAnnotationDialog.showForDomainObjects(selected, true);

        });
        toolBar.add(bulkPermissionsButton);
        
        return toolBar;
    }

    private void showSelectionMessage() {
        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                "Select some items to bulk-edit permissions", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void runDefaultNodeAction(Node node) {
        if (node instanceof OntologyTermNode) {
            log.info("Running action for node@{} -> {}",System.identityHashCode(node),node.getDisplayName());
            OntologyTermNode termNode = (OntologyTermNode)node;
            final ApplyAnnotationActionListener action = new ApplyAnnotationActionListener();
            action.performAction(termNode.getOntologyTerm());
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

    public void showKeyBindDialog(OntologyTerm term) {
        OntologyElementAction action = getActionForTerm(term);
        if (action!=null) {
            getKeyBindDialog().showForAction(action);
        }
    }

    public void executeBinding(OntologyTerm term) {

        for (IdentifiableNode identifiableNode : NodeTracker.getInstance().getNodesById(term.getId())) {

            // Now we can select the node we actually want
            Long[] path = NodeUtils.createIdPath(identifiableNode);
            OntologyTermNode node = select(path);
            if (node!=null) {
                beanTreeView.selectNode(node);

                ApplyAnnotationActionListener action = new ApplyAnnotationActionListener(Arrays.asList(term));
                action.actionPerformed(null);

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
                    for (IdentifiableNode node : NodeTracker.getInstance().getNodesById(existingAction2.getOntologyTermId())) {
                        if (node instanceof OntologyTermNode) {
                            ((OntologyTermNode)node).fireShortcutChanged();
                        }
                    }
                }
            }
            
            // Refresh the nodes gaining the keybind
            for (IdentifiableNode node : NodeTracker.getInstance().getNodesById(action.getOntologyTermId())) {
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
                            log.info("  Binding {} to {}", action.getOntologyTerm().getName(), shortcut);
                            KeyBindings.getKeyBindings().setBinding(shortcut, action, false);
                        }
                        else {
                            log.info("  Could not find OntologyTerm#{} for binding {}", bind.getOntologyTermId(), shortcut);
                            // TODO: in the future we should clean these up
                        }
                    }
                    catch (Exception e) {
                        log.error("Could not load key binding from user preference '" + bind.getKey() + "'.", e);
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Could not load user's key binding preferences");
            FrameworkAccess.handleException(e);
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
            FrameworkAccess.handleException(e);
        }
    }
}
