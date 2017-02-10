package org.janelia.it.workstation.gui.top_component;

import java.util.Properties;

import javax.swing.JTextArea;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays the icon demo panel viewers.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.dialogs.nb//IconPanel//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = IconPanelTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false, position = 0)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.dialogs.nb.IconPanelTopComponent")
@ActionReference(path = "Menu/Window/Legacy", position = 12)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_IconPanelAction",
        preferredID = IconPanelTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_IconPanelAction=Legacy Browser",
    "CTL_IconPanelTopComponent=Legacy Browser",
    "HINT_IconPanelTopComponent=Data shown as an array of graphical icons"
})
public final class IconPanelTopComponent extends TopComponent {
    
    private Logger log = LoggerFactory.getLogger( EntityDetailsTopComponent.class );
    
    public static final String PREFERRED_ID = "IconPanelTopComponent";

    public IconPanelTopComponent() {
        initComponents();
        setName(Bundle.CTL_IconPanelTopComponent());
        setToolTipText(Bundle.HINT_IconPanelTopComponent());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setLayout(new java.awt.BorderLayout());
        JTextArea area = new JTextArea("The Legacy Browser is no longer available. Please use Window->Core->Data Inspector instead.");
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        add(area);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void componentOpened() {
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
}