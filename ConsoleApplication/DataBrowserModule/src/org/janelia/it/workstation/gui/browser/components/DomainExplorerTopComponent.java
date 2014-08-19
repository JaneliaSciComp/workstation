package org.janelia.it.workstation.gui.browser.components;

import org.janelia.it.workstation.gui.browser.api.DomainDAO;

import java.util.Collection;

import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.nodes.TreeNodeNode;
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
        dtd = "-//org.janelia.it.FlyWorkstation.gui.dialogs.nb//DomainExplorer//EN",
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
        preferredID = "DomainExplorerTopComponent"
)
@Messages({
    "CTL_DomainExplorerAction=Domain Explorer",
    "CTL_DomainExplorerTopComponent=Domain Explorer",
    "HINT_DomainExplorerTopComponent=Browse the data"
})
public final class DomainExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private Logger log = LoggerFactory.getLogger(DomainExplorerTopComponent.class);

    public static final String TC_NAME = "DomainExplorerTopComponent";
    
    protected static final String MONGO_SERVER_URL = "rokicki-ws";
    protected static final String MONGO_DATABASE = "jacs";

    private final ExplorerManager mgr = new ExplorerManager();

    public DomainExplorerTopComponent() {
        initComponents();
        beanTreeView.setDefaultActionAllowed(false);
        
        setName(Bundle.CTL_DomainExplorerTopComponent());
        setToolTipText(Bundle.HINT_DomainExplorerTopComponent());
        associateLookup(ExplorerUtils.createLookup(mgr, getActionMap()));

        ActionMap map = this.getActionMap();
        map.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(mgr));
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(mgr));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(mgr));
        map.put("delete", ExplorerUtils.actionDelete(mgr, true)); 

        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                loadWorkspaces();
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

    private WorkspaceWrapper currWorkspace;

    private void loadWorkspaces() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    DomainDAO dao = getDao();
                    Collection<Workspace> workspaces = dao.getWorkspaces(SessionMgr.getSubjectKey());
                    DefaultComboBoxModel<WorkspaceWrapper> model = new DefaultComboBoxModel<WorkspaceWrapper>();
                    for (Workspace workspace : workspaces) {
                        WorkspaceWrapper wrapper = new WorkspaceWrapper(workspace);
                        model.addElement(wrapper);
                        if (currWorkspace==null && workspace.getOwnerKey().equals(SessionMgr.getSubjectKey())) {
                            currWorkspace = wrapper;
                        }
                    }
                    model.setSelectedItem(currWorkspace);
                    workspaceCombo.setModel(model);
                    loadWorkspace(currWorkspace.getWorkspace());
                }
                catch (Exception e) {
                    SessionMgr.getSessionMgr().handleException(e);
                }
            }
        });
    }

    private void loadWorkspace(Workspace workspace) throws Exception {
        mgr.setRootContext(new TreeNodeNode(null, workspace));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        beanTreeView = new org.openide.explorer.view.BeanTreeView();
        toolBar = new javax.swing.JToolBar();
        workspaceCombo = new javax.swing.JComboBox();
        refreshButton = new javax.swing.JButton();
        viewToggleButton = new javax.swing.JToggleButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));

        toolBar.setRollover(true);

        workspaceCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Loading..." }));
        workspaceCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                workspaceComboItemStateChanged(evt);
            }
        });
        toolBar.add(workspaceCombo);

        refreshButton.setIcon(Icons.getRefreshIcon());
        org.openide.awt.Mnemonics.setLocalizedText(refreshButton, org.openide.util.NbBundle.getMessage(DomainExplorerTopComponent.class, "DomainExplorerTopComponent.refreshButton.text")); // NOI18N
        refreshButton.setFocusable(false);
        refreshButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        refreshButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });
        toolBar.add(refreshButton);

        org.openide.awt.Mnemonics.setLocalizedText(viewToggleButton, org.openide.util.NbBundle.getMessage(DomainExplorerTopComponent.class, "DomainExplorerTopComponent.viewToggleButton.text")); // NOI18N
        viewToggleButton.setToolTipText(org.openide.util.NbBundle.getMessage(DomainExplorerTopComponent.class, "DomainExplorerTopComponent.viewToggleButton.toolTipText")); // NOI18N
        viewToggleButton.setFocusable(false);
        viewToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        viewToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        viewToggleButton.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                viewToggleButtonStateChanged(evt);
            }
        });
        toolBar.add(viewToggleButton);
        toolBar.add(filler1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(beanTreeView, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
            .addComponent(toolBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(beanTreeView, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void workspaceComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_workspaceComboItemStateChanged
        try {
            WorkspaceWrapper wrapper = (WorkspaceWrapper) evt.getItem();
            loadWorkspace(wrapper.getWorkspace());
        }
        catch (Exception e) {
            log.error("Error changing workspace", e);
        }
    }//GEN-LAST:event_workspaceComboItemStateChanged

    private static boolean showNeuronView = false;

    public static boolean isShowNeurons() {
        return showNeuronView;
    }

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        loadWorkspaces();
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void viewToggleButtonStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_viewToggleButtonStateChanged

        try {
            if (showNeuronView != viewToggleButton.isSelected()) {
                showNeuronView = viewToggleButton.isSelected();
                loadWorkspace(currWorkspace.getWorkspace());
            }
        }
        catch (Exception e) {
            log.error("Error changing view", e);
        }
    }//GEN-LAST:event_viewToggleButtonStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.openide.explorer.view.BeanTreeView beanTreeView;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JButton refreshButton;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JToggleButton viewToggleButton;
    private javax.swing.JComboBox workspaceCombo;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
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

    private class WorkspaceWrapper {

        private Workspace workspace;

        WorkspaceWrapper(Workspace workspace) {
            this.workspace = workspace;
        }

        public Workspace getWorkspace() {
            return workspace;
        }

        public String toString() {
            return workspace.getName() + " (" + workspace.getOwnerKey() + ")";
        }
    }
}
