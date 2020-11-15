package org.janelia.workstation.gui.passive_3d.top_component;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@ConvertAsProperties(
        dtd = "-//org.janelia.workstation.gui.passive_3d.top_component//Snapshot3d//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = Snapshot3dTopComponent.SNAPSHOT3D_TOP_COMPONENT_PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false, position = 33)
@ActionID(category = "Window", id = "Snapshot3dTopComponent")
@ActionReference(path = "Menu/Window/Large Volume Viewer", position = 102)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_Snapshot3dAction",
        preferredID = Snapshot3dTopComponent.SNAPSHOT3D_TOP_COMPONENT_PREFERRED_ID
)
@Messages({
    "CTL_Snapshot3dAction=Snapshot 3d",
    "CTL_Snapshot3dTopComponent=" + Snapshot3dTopComponent.LABEL_TEXT,
    "HINT_Snapshot3dTopComponent=Large Volume Viewer's snapshot of data containing the crosshair."
})
public final class Snapshot3dTopComponent extends TopComponent {
    public static final String SNAPSHOT3D_TOP_COMPONENT_PREFERRED_ID = "Snapshot3dTopComponent";
    public static final String LABEL_TEXT = "  LVV 3D Snapshot  ";
    
    private JComponent externallySuppliedComponent = null;

    public Snapshot3dTopComponent() {
        initComponents();
        setName(Bundle.CTL_Snapshot3dTopComponent());
        setToolTipText(Bundle.HINT_Snapshot3dTopComponent());

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        containerPanel = new javax.swing.JPanel();

        jPanel1.setLayout(new java.awt.BorderLayout());

        containerPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(containerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 398, Short.MAX_VALUE))
            .addComponent(containerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel containerPanel;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // Must use this opportunity for cleanup of any lingering viewer.
        cleanupContent();
    }

    @Override
    public void componentClosed() {
        cleanupContent();
    }

    public void cleanupContent() {
        if ( externallySuppliedComponent != null ) {
            containerPanel.remove( externallySuppliedComponent );
            externallySuppliedComponent = null;
        }
    }

    public void setSnapshotComponent( JComponent component ) {
        externallySuppliedComponent = component;
        containerPanel.add( component, BorderLayout.CENTER );
        //jPanel1.add( component, BorderLayout.CENTER );
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
    
}