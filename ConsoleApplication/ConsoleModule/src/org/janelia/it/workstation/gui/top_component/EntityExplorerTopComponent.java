package org.janelia.it.workstation.gui.top_component;

import java.awt.BorderLayout;
import java.util.Properties;

import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.outline.EntityOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays the data explorer. 
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.dialogs.nb//EntityExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = EntityExplorerTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false, position = 10)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.dialogs.nb.EntityExplorerTopComponent")
@ActionReference(path = "Menu/Window", position = 400)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_EntityExplorerAction",
        preferredID = EntityExplorerTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_EntityExplorerAction=Legacy Data Explorer",
    "CTL_EntityExplorerTopComponent=Legacy Data Explorer",
    "HINT_EntityExplorerTopComponent=Browse the data"
})
public final class EntityExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private Logger log = LoggerFactory.getLogger( EntityExplorerTopComponent.class );
    
    public static final String PREFERRED_ID = "EntityExplorerTopComponent";
    
    private ExplorerManager mgr = new ExplorerManager();
    
    public EntityExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_EntityExplorerTopComponent());
        setToolTipText(Bundle.HINT_EntityExplorerTopComponent());
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);   
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();

        jPanel1.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void componentOpened() {
        final Browser browser = SessionMgr.getBrowser();
        if (browser == null) {
            throw new IllegalStateException("Failed to obtain browser object for component.");
        }
        final EntityOutline entityOutline = browser.getEntityOutline();
        if (entityOutline == null) {
            throw new IllegalStateException("No entity outline located.");
        }
        else {
            log.debug("Activating entity outline");
            entityOutline.activate();
            jPanel1.add(entityOutline, BorderLayout.CENTER);
        }
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }
}
