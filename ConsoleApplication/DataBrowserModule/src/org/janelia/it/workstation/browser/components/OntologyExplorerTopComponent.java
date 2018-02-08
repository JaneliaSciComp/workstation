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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Position;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.api.KeyBindings;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.events.Events;
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
import org.janelia.it.workstation.browser.gui.keybind.KeyboardShortcut;
import org.janelia.it.workstation.browser.gui.keybind.KeymapUtil;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.JScrollPopupMenu;
import org.janelia.it.workstation.browser.gui.support.MouseForwarder;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.gui.tree.CustomTreeToolbar;
import org.janelia.it.workstation.browser.gui.tree.CustomTreeView;
import org.janelia.it.workstation.browser.nb_action.NewOntologyActionListener;
import org.janelia.it.workstation.browser.nodes.EmptyNode;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.OntologyNode;
import org.janelia.it.workstation.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.browser.util.ConcurrentUtils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.ontology.Ontology;
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

import com.google.common.eventbus.Subscribe;
import org.janelia.it.workstation.browser.events.lifecycle.SessionStartEvent;

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
    
    public static OntologyExplorerTopComponent getInstance() {
        return (OntologyExplorerTopComponent)WindowLocator.getByName(OntologyExplorerTopComponent.TC_NAME);
    }

    private final CustomTreeToolbar toolbar;
    private final JPanel centerPanel;
    private final JPanel treePanel;
    private final CustomTreeView beanTreeView;
    private final FindToolbar findToolbar;
    
    private final ExplorerManager mgr = new ExplorerManager();
    private final KeyListener keyListener;
    private final KeyBindDialog keyBindDialog;
    private final BulkAnnotationPermissionDialog bulkAnnotationDialog;
    private final AutoAnnotationPermissionDialog autoAnnotationDialog;
    private final Debouncer debouncer = new Debouncer();
    
    private final List<Ontology> ontologies = new ArrayList<>();
    
    private OntologyNode ontologyNode;
    private boolean recordingKeyBinds = false;
    private boolean loadInitialState = true;
        
    public OntologyExplorerTopComponent() {
        initComponents();
        
        this.beanTreeView = new CustomTreeView(this);
        
        setName(Bundle.CTL_OntologyExplorerTopComponent());
        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 
        
        this.toolbar = new CustomTreeToolbar(beanTreeView) {
            @Override
            protected void refresh() {
                OntologyExplorerTopComponent.this.refresh();
            }
        };
        this.treePanel = new JPanel(new BorderLayout());
        this.centerPanel = new JPanel(new BorderLayout());
        this.findToolbar = new FindToolbar(this);

        centerPanel.add(treePanel, BorderLayout.CENTER);
        centerPanel.add(findToolbar, BorderLayout.PAGE_END);
        
        add(toolbar, BorderLayout.PAGE_START);
        add(centerPanel, BorderLayout.CENTER);
        add(getBottomToolbar(), BorderLayout.PAGE_END);
        
        this.bulkAnnotationDialog = new BulkAnnotationPermissionDialog();
        this.autoAnnotationDialog = new AutoAnnotationPermissionDialog();
        
        // Create input listeners which will be added to the DynamicTree later
        this.keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if (KeymapUtil.isModifier(e)) {
                        return;
                    }
                    
                    KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
                    Node currNode = beanTreeView.getCurrentNode();
                    
                    if (recordingKeyBinds && ontologyNode!=null && currNode!=null) {
                        
                        log.debug("User pressed "+e.getKeyChar());
                        e.consume();
                    
                        if (currNode instanceof OntologyTermNode) {
                            
                            log.debug("Rebinding current node: {}",currNode.getDisplayName());
                            org.janelia.it.workstation.browser.actions.Action action = ontologyNode.getActionForNode((OntologyTermNode)currNode);

                            if (action == null) {
                                throw new IllegalStateException("No action for current node");
                            }
    
                            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                                // Clear the key binding
                                KeyBindings.getKeyBindings().setBinding(null, action);
                            }
                            else {
                                // Set the key binding
                                KeyBindings.getKeyBindings().setBinding(shortcut, action);
                            }
    
                            // Refresh the entire tree (another key bind may have been overridden)
                            refresh(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    // Move to the next row
                                    beanTreeView.navigateToNextRow();
                                    return null;
                                }
                            });
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
        beanTreeView.replaceKeyListeners(keyListener);
        
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
    }

    @Override
    public void componentClosed() {
        Events.getInstance().unregisterOnEventBus(this);
    }

    @Override
    protected void componentActivated() {
        log.info("Activating ontology explorer");
        FindContextManager.getInstance().activateContext(this);
    }
    
    @Override
    protected void componentDeactivated() {
        FindContextManager.getInstance().deactivateContext(this);
    }
    
    void writeProperties(java.util.Properties p) {
    }

    void readProperties(java.util.Properties p) {
        if (AccessManager.loggedIn()) {
            loadInitialState();
        }
        else {
            // Not logged in yet, wait for a SessionStartEvent
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
        loadInitialState();
    }
    
    private void loadInitialState() {
        
        if (!loadInitialState) return;
        log.info("Loading initial session");
        this.loadInitialState = false;
        
        showLoadingIndicator();
        
        if (!debouncer.queue(null)) {
            log.debug("Skipping initial load, since there is a refresh already in progress");
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadOntologies();
            }

            @Override
            protected void hadSuccess() {
                Long ontologyId = StateMgr.getStateMgr().getCurrentOntologyId();
                Events.getInstance().postOnEventBus(new OntologySelectionEvent(ontologyId));
                debouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                showNothing();
                ConsoleApp.handleException(error);
                debouncer.failure();
            }
        };
        
        worker.execute();
    }
    
    @Subscribe
    public void ontologySelected(OntologySelectionEvent event) {
        Long ontologyId = event.getOntologyId();
        log.trace("selectOntology({})",ontologyId);
        selectOntology(ontologyId);
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
                    if (ontologyNode!=null && ontologyNode.getId().equals(updatedOntology.getId())) {
                        // Current ontology has been invalidated 
                        log.info("Refreshing because current ontology '{}' has been invalidated.",updatedOntology.getName());
                        refresh(false, true, null);
                        break;
                    }
                    else {
                        Integer replaceIndex = null;
                        int i = 0;
                        for(Ontology ontology : ontologies) {
                            if (updatedOntology.getId().equals(ontology.getId())) {
                                replaceIndex = i;
                                break;
                            }
                            i++;
                        }
                        if (replaceIndex!=null) {
                            log.info("Updating invalidated ontology {} at {}",updatedOntology.getName(),replaceIndex);
                            ontologies.set(replaceIndex, updatedOntology);
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void objectDeleted(DomainObjectRemoveEvent event) {
        if (event.getDomainObject() instanceof Ontology) {
            Ontology deletedOntology = (Ontology)event.getDomainObject();
            log.info("Removing ontology '{}' from view.", deletedOntology);
            ontologies.remove(deletedOntology);
            if (ontologyNode!=null && ontologyNode.getId().equals(deletedOntology.getId())) {
                showOntology(null);
            }
        }
    }

    @Subscribe
    public void objectCreated(DomainObjectCreateEvent event) {
        final DomainObject domainObject = event.getDomainObject();
        if (domainObject instanceof Ontology) {
            log.info("Refreshing because a new ontology '{}' was created.", domainObject.getName());
            refresh(false, true, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    selectOntology(domainObject.getId());
                    return null;
                }
            });
        }
    }

    @Subscribe
    public void objectChanged(DomainObjectChangeEvent event) {
        final DomainObject domainObject = event.getDomainObject();
        if (ontologyNode != null && ontologyNode.getId().equals(domainObject.getId())) {
            // Current ontology has changed
            log.info("Refreshing because current ontology '{}' has changed.", domainObject.getName());
            refresh(false, true, null);
        }
    }

    private void selectOntology(Long ontologyId) {
        selectOntology(ontologyId, null, null);
    }
    
    private void selectOntology(Long ontologyId, List<Long[]> expanded, List<Long[]> selected) {
        log.debug("selectOntology({})", ontologyId);
        if (ontologyId==null) {
            showOntology(null);
        }
        else {
            boolean found = false;
            for (Ontology ontology : ontologies) {
                if (ontology.getId().equals(ontologyId)) {
                    showOntology(ontology);
                    found = true;
                }
            }
            if (!found) {
                log.warn("Selected ontology not found: {}",ontologyId);
                showNothing();
            }
        }

        // Ensure the updated tree is visible before expanding nodes
        beanTreeView.updateUI();
        
        SwingUtilities.invokeLater(() -> {
            log.info("Expanding all");
            beanTreeView.expandAll();    
            // Restore tree state
            if (expanded!=null) {
                beanTreeView.expand(expanded);
            }
            if (selected!=null) {
                beanTreeView.selectPaths(selected);
            }
        });
    }
    
    private void showOntology(Ontology ontology) {
        log.debug("showOntology({})", ontology);
        
        if (ontology==null) {
            this.ontologyNode = null;
            mgr.setRootContext(new EmptyNode("No ontology selected"));
        }
        else {
            this.ontologyNode = new OntologyNode(ontology);
            if (!recordingKeyBinds) {
                KeyBindings.getKeyBindings().loadOntologyKeybinds(ontology.getId(), ontologyNode.getOntologyActionMap());
            }
            mgr.setRootContext(ontologyNode);
        }
        
        if (ontology==null) {
            showNothing();
        }
        else {
            showTree();
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
        
        final List<Long[]> expanded = ontologyNode!=null && restoreState ? beanTreeView.getExpandedPaths() : null;
        final List<Long[]> selected = ontologyNode!=null && restoreState ? beanTreeView.getSelectedPaths() : null;

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                if (invalidateCache) {
                    DomainModel model = DomainMgr.getDomainMgr().getModel();
                    model.invalidateAll();
                }
                loadOntologies();
            }

            @Override
            protected void hadSuccess() {
                try {
                    if (ontologyNode!=null) {
                        // Reselect the current ontology
                        selectOntology(ontologyNode.getId(), expanded, selected);
                    }
                    beanTreeView.grabFocus();
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
                ConsoleApp.handleException(error);
            }
        };
        
        worker.execute();
    }
    
    public OntologyTermNode select(Long[] idPath) {
        if (ontologyNode==null) return null;
        Node node = NodeUtils.findNodeWithPath(ontologyNode, idPath);
        log.info("Found node with path {}: {}",NodeUtils.createPathString(idPath),node.getDisplayName());
        beanTreeView.selectNode(node);
        return (OntologyTermNode)node;
    }
    
    private void loadOntologies() throws Exception {
        ontologies.clear();
        ontologies.addAll(DomainMgr.getDomainMgr().getModel().getOntologies());
    }
    
    public KeyBindDialog getKeyBindDialog() {
        return keyBindDialog;
    }
    
    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }
    
    public OntologyNode getOntologyNode() {
        return ontologyNode;
    }
    
    private JToolBar getBottomToolbar() {

        final Component mainFrame = ConsoleApp.getMainFrame();
    
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
                        JMenuItem roleMenuItem = new JCheckBoxMenuItem(ontology.getName() + " (" + owner + ")", checked);
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
                    KeyBindings.getKeyBindings().saveOntologyKeybinds(ontologyNode.getId());
                    ActivityLogHelper.logUserAction("OntologyExplorerTopComponent.exitKeyBindingMode");
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
}
