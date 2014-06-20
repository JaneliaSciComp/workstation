/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.browser.components;

import java.util.Collection;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import org.janelia.it.jacs.model.domain.Ontology;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import static org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent.MONGO_SERVER_URL;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.browser.components//OntologyExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "OntologyExplorerTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = true, position = 500)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_OntologyExplorerAction",
        preferredID = "OntologyExplorerTopComponent"
)
@Messages({
    "CTL_OntologyExplorerAction=Ontology Explorer",
    "CTL_OntologyExplorerTopComponent=Ontology Explorer",
    "HINT_OntologyExplorerTopComponent=Use ontologies to annotate your data"
})
public final class OntologyExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private Logger log = LoggerFactory.getLogger(OntologyExplorerTopComponent.class);
    
    protected static final String MONGO_SERVER_URL = "rokicki-ws";
    protected static final String MONGO_DATABASE = "jacs";
    
    private ExplorerManager mgr = new ExplorerManager();
    
    public OntologyExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_OntologyExplorerTopComponent());
        setToolTipText(Bundle.HINT_OntologyExplorerTopComponent());
        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));

        ActionMap map = this.getActionMap ();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 

        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                loadOntologies();
            }
        });
    }

    private static DomainDAO dao;

    public static DomainDAO getDao() {
        if (dao == null) {
            try {
                dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        return dao;
    }

    private OntologyWrapper currOntology;

    private void loadOntologies() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    DomainDAO dao = getDao();
                    Collection<Ontology> workspaces = dao.getOntologies(SessionMgr.getSubjectKey());
                    DefaultComboBoxModel<OntologyWrapper> model = new DefaultComboBoxModel<OntologyWrapper>();
                    for (Ontology workspace : workspaces) {
                        OntologyWrapper wrapper = new OntologyWrapper(workspace);
                        model.addElement(wrapper);
                        if (workspace.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
                            currOntology = wrapper;
                            break;
                        }
                    }
                    model.setSelectedItem(currOntology);
                    ontologyCombo.setModel(model);
                    loadOntology(currOntology.getOntology());
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
    }

    private void loadOntology(Ontology workspace) throws Exception {
        mgr.setRootContext(new OntologyNode(null, workspace));
    }
    
    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar = new javax.swing.JToolBar();
        ontologyCombo = new javax.swing.JComboBox();
        refreshButton = new javax.swing.JButton();
        setShortcutsButton = new javax.swing.JToggleButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0));
        beanTreeView = new org.openide.explorer.view.BeanTreeView();

        jToolBar.setRollover(true);
        jToolBar.setName(""); // NOI18N

        ontologyCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Loading..." }));
        ontologyCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ontologyComboItemStateChanged(evt);
            }
        });
        jToolBar.add(ontologyCombo);

        refreshButton.setIcon(Icons.getRefreshIcon());
        org.openide.awt.Mnemonics.setLocalizedText(refreshButton, org.openide.util.NbBundle.getMessage(OntologyExplorerTopComponent.class, "DomainExplorerTopComponent.refreshButton.text")); // NOI18N
        refreshButton.setFocusable(false);
        refreshButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        refreshButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });
        jToolBar.add(refreshButton);

        org.openide.awt.Mnemonics.setLocalizedText(setShortcutsButton, org.openide.util.NbBundle.getMessage(OntologyExplorerTopComponent.class, "DomainExplorerTopComponent.viewToggleButton.text")); // NOI18N
        setShortcutsButton.setToolTipText(org.openide.util.NbBundle.getMessage(OntologyExplorerTopComponent.class, "DomainExplorerTopComponent.viewToggleButton.toolTipText")); // NOI18N
        setShortcutsButton.setActionCommand("");
        setShortcutsButton.setFocusable(false);
        setShortcutsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        setShortcutsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        setShortcutsButton.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                setShortcutsButtonStateChanged(evt);
            }
        });
        jToolBar.add(setShortcutsButton);
        jToolBar.add(filler2);
        jToolBar.add(filler1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(beanTreeView, javax.swing.GroupLayout.DEFAULT_SIZE, 596, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(beanTreeView, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void ontologyComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ontologyComboItemStateChanged
        try {
            OntologyWrapper wrapper = (OntologyWrapper) evt.getItem();
            loadOntology(wrapper.getOntology());
        }
        catch (Exception e) {
            log.error("Error changing workspace", e);
        }
    }//GEN-LAST:event_ontologyComboItemStateChanged

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        loadOntologies();
    }//GEN-LAST:event_refreshButtonActionPerformed

    private static boolean isSettingShortcuts = false;

    private void setShortcutsButtonStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_setShortcutsButtonStateChanged
        
        try {
            if (isSettingShortcuts != setShortcutsButton.isSelected()) {
                isSettingShortcuts = setShortcutsButton.isSelected();
                // TODO: toggle setting of shortcuts
            }
        }
        catch (Exception e) {
            log.error("Error changing view", e);
        }
    }//GEN-LAST:event_setShortcutsButtonStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.openide.explorer.view.BeanTreeView beanTreeView;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JToolBar jToolBar;
    private javax.swing.JComboBox ontologyCombo;
    private javax.swing.JButton refreshButton;
    private javax.swing.JToggleButton setShortcutsButton;
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
    
    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }
    
    private class OntologyWrapper {

        private Ontology ontology;

        OntologyWrapper(Ontology ontology) {
            this.ontology = ontology;
        }

        public Ontology getOntology() {
            return ontology;
        }

        public String toString() {
            return ontology.getName() + " (" + ontology.getOwnerKey() + ")";
        }
    }
}
