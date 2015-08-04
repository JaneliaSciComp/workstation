package org.janelia.it.workstation.gui.browser.components;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.gui.dialogs.AutoAnnotationPermissionDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.BulkAnnotationPermissionDialog;
import org.janelia.it.workstation.gui.browser.gui.dialogs.KeyBindDialog;
import org.janelia.it.workstation.gui.browser.model.DomainObjectComparator;
import org.janelia.it.workstation.gui.browser.nodes.CustomTreeToolbar;
import org.janelia.it.workstation.gui.browser.nodes.CustomTreeView;
import org.janelia.it.workstation.gui.browser.nodes.NodeUtils;
import org.janelia.it.workstation.gui.browser.nodes.OntologyNode;
import org.janelia.it.workstation.gui.browser.nodes.OntologyTermNode;
import org.janelia.it.workstation.gui.browser.nodes.RootNode;
import org.janelia.it.workstation.gui.framework.actions.Action;
import org.janelia.it.workstation.gui.framework.actions.CreateOntologyAction;
import org.janelia.it.workstation.gui.framework.actions.ImportOWLOntologyAction;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.JScrollPopupMenu;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.WindowLocator;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component for the Ontology Editor, which lets users create ontologies
 * and annotate their domain objects.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.browser.components//OntologyExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = OntologyExplorerTopComponent.TC_NAME,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_OntologyExplorerAction",
        preferredID = OntologyExplorerTopComponent.TC_NAME
)
@Messages({
    "CTL_OntologyExplorerAction=Ontology Explorer",
    "CTL_OntologyExplorerTopComponent=Ontology Explorer",
    "HINT_OntologyExplorerTopComponent=Use ontologies to annotate your data"
})
public final class OntologyExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private Logger log = LoggerFactory.getLogger(OntologyExplorerTopComponent.class);

    public static final String TC_NAME = "OntologyExplorerTopComponent";
    
    public static OntologyExplorerTopComponent getInstance() {
        return (OntologyExplorerTopComponent)WindowLocator.getByName(OntologyExplorerTopComponent.TC_NAME);
    }
    
    private final CustomTreeView beanTreeView;
    private final ExplorerManager mgr = new ExplorerManager();
    private final KeyListener keyListener;
    private final KeyBindDialog keyBindDialog;
    private final BulkAnnotationPermissionDialog bulkAnnotationDialog;
    private final AutoAnnotationPermissionDialog autoAnnotationDialog;
    
    private final List<Ontology> ontologies = new ArrayList<>();
    
    private final Map<String, Action> ontologyActionMap = new HashMap<>();
    private Ontology currOntology;
    private RootNode root;
    private boolean recordingKeyBinds = false;
    
    
    public OntologyExplorerTopComponent() {
        initComponents();
        
        this.beanTreeView = new CustomTreeView(this);
        
        setName(Bundle.CTL_OntologyExplorerTopComponent());
        
        setToolTipText(Bundle.HINT_OntologyExplorerTopComponent());
        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 
        
        CustomTreeToolbar toolbar = new CustomTreeToolbar(beanTreeView) {
            protected void refresh() {
                OntologyExplorerTopComponent.this.refresh();
            }
        };
        add(toolbar, BorderLayout.PAGE_START);
        add(beanTreeView, BorderLayout.CENTER);
        
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
                    
                    log.info("hit "+e.getKeyChar()+" on "+currNode+" recordingKeyBinds="+recordingKeyBinds);
                    
                    if (recordingKeyBinds && currNode!=null) {
                        if (currNode instanceof OntologyTermNode) {
                            Action action = getActionForNode((OntologyTermNode)currNode);
    
                            if (action == null) {
                                throw new IllegalStateException("No action for current node");
                            }
    
                            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                                // Clear the key binding
                                SessionMgr.getKeyBindings().setBinding(null, action);
                            }
                            else {
                                // Set the key binding
                                SessionMgr.getKeyBindings().setBinding(shortcut, action);
                            }
    
                            // Refresh the entire tree (another key bind may have been overridden)
                            refresh();
                        }

                        // Move to the next row
                        beanTreeView.navigateToNextRow();
                    }
                    else {
                        SessionMgr.getKeyBindings().executeBinding(shortcut);
                    }
                }
            }
        };

        beanTreeView.replaceKeyListeners(keyListener);
        
        // Prepare the key binding dialog box
        // TODO: fix this
        this.keyBindDialog = new KeyBindDialog();
        keyBindDialog.pack();

        keyBindDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                refresh();
            }
        });
        
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                loadOntologies();
            }
        });
    }

    private void loadOntologies() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    DomainDAO dao = DomainMgr.getDomainMgr().getDao();
                    for (Ontology ontology : dao.getOntologies(SessionMgr.getSubjectKey())) {
                        ontologies.add(ontology);
                        if (currOntology==null && ontology.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
                            currOntology = ontology;
                        }
                    }
                    Collections.sort(ontologies, new DomainObjectComparator());
                    if (currOntology!=null) {
                        loadOntology(currOntology);
                    }
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
        // Refresh popup menu
        add(getBottomToolbar(), BorderLayout.PAGE_END);
    }

    private void loadOntology(Ontology ontology) {
        this.currOntology = ontology;
        mgr.setRootContext(new OntologyNode(ontology));        
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
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
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
    
    // Custom methods
    
    public void refresh() {
        refresh(null);
    }
    
    public void refresh(final Callable<Void> success) {
                        
        final List<Long[]> expanded = beanTreeView.getExpandedPaths();
        
        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                loadOntologies();
            }

            @Override
            protected void hadSuccess() {
                try {
                    beanTreeView.expand(expanded);
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
    
    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }
    
    public RootNode getRoot() {
        return root;
    }
    
    public Action getActionForNode(OntologyTermNode node) {
        if (ontologyActionMap == null) {
            return null;
        }
        String idPathStr = NodeUtils.createPathString(node);
        return ontologyActionMap.get(idPathStr);
    }
    
    protected JToolBar getBottomToolbar() {

        final Component mainFrame = SessionMgr.getMainFrame();
    
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        log.info("Generating toolbar");
        
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

                    for (final Ontology ontology : ontologies) {
                        Subject subject = null;
                        try {
                            subject = ModelMgr.getModelMgr().getSubjectByKey(ontology.getOwnerKey());
                        }
                        catch (Exception ex) {
                            log.error("Error getting subject: "+ontology.getOwnerKey(),ex);
                        }
                        String owner = subject==null?ontology.getOwnerKey():subject.getFullName();
                        boolean checked = currOntology != null && ontology.getId().equals(currOntology.getId());
                        JMenuItem roleMenuItem = new JCheckBoxMenuItem(ontology.getName() + " (" + owner + ")", checked);
                        String iconName = DomainUtils.isOwner(ontology)?"folder.png":"folder_blue.png";
                        roleMenuItem.setIcon(Icons.getIcon(iconName));
                        roleMenuItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                // TODO: use events, like before
                                //ModelMgr.getModelMgr().setCurrentOntologyId(ontology.getId());
                                loadOntology(ontology);
                            }
                        });
                        ontologyListMenu.add(roleMenuItem);
                    }

                    ontologyListMenu.add(new JSeparator());

                    JMenuItem addMenuItem = new JMenuItem("Create New Ontology...");
                    addMenuItem.setIcon(Icons.getIcon("folder_add.png"));
                    addMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Action action = new CreateOntologyAction();
                            action.doAction();
                        }
                    });
                    ontologyListMenu.add(addMenuItem);

                    JMenuItem loadOwlItem = new JMenuItem("Load OWL File...");
                    loadOwlItem.setIcon(Icons.getIcon("folder_add.png"));
                    loadOwlItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Action action = new ImportOWLOntologyAction();
                            action.doAction();
                        }
                    });
                    ontologyListMenu.add(loadOwlItem);

                    ontologyListMenu.show(ontologyButton, 0, ontologyButton.getHeight());
                }
            });
            ontologyButton.addMouseListener(new MouseForwarder(toolBar, "OntologyButton->JToolBar"));
            toolBar.add(ontologyButton);
        }

        final JToggleButton keyBindButton = new JToggleButton();
        keyBindButton.setIcon(Icons.getIcon("keyboard_add.png"));
        keyBindButton.setToolTipText("Enter key binding mode");
        keyBindButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (keyBindButton.isSelected()) {
                    keyBindButton.setToolTipText("Exit key binding mode");
                    recordingKeyBinds = true;
                    // Transfer focus to a node in the tree in preparation for key presses
                    beanTreeView.grabFocus();
                }
                else {
                    keyBindButton.setToolTipText("Enter key binding mode");
                    recordingKeyBinds = false;
                    // TODO: save from Ontology instead of Entity
                    //SessionMgr.getKeyBindings().saveOntologyKeybinds();
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
                        PermissionTemplate template = SessionMgr.getBrowser().getAutoShareTemplate();
                        if (template!=null) {
                            JOptionPane.showMessageDialog(mainFrame,
                                "Auto-sharing annotation with "+
                                EntityUtils.getNameFromSubjectKey(template.getSubjectKey()), 
                                "Auto-sharing ended", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
                else {
                    SessionMgr.getBrowser().setAutoShareTemplate(null);
                    JOptionPane.showMessageDialog(mainFrame,
                        "No longer auto-sharing annotations", "Auto-sharing ended", JOptionPane.INFORMATION_MESSAGE);
                }
                
                autoShareButton.setSelected(SessionMgr.getBrowser().getAutoShareTemplate()!=null);
            }
            
        });
        autoShareButton.setSelected(SessionMgr.getBrowser().getAutoShareTemplate()!=null);
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
}
