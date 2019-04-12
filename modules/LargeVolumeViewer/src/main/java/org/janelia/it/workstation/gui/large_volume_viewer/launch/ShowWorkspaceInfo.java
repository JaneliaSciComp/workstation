package org.janelia.it.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * right-click on workspace in Data Explorer, get info on its sample
 */
@ServiceProvider(service = ObjectOpenAcceptor.class, path = ObjectOpenAcceptor.LOOKUP_PATH)
public class ShowWorkspaceInfo implements ObjectOpenAcceptor  {

    private static final Logger log = LoggerFactory.getLogger(ShowWorkspaceInfo.class);
    
    private static final int MENU_ORDER = 400;
    
    @Override
    public void acceptObject(Object domainObject) {
    	
    	TmWorkspace workspace = (TmWorkspace)domainObject;
    	
        TmSample sample = null;
        try {
        	sample = DomainMgr.getDomainMgr().getModel().getDomainObject(workspace.getSampleRef());
        } catch (Exception e) {
            log.error("Error getting sample "+workspace.getSampleRef(),e);
        }
        String title;
        String message = "Workspace name: " + workspace.getName() + "\n";
        if (sample == null) {
            title = "Error";
            message += "\nCould not retrieve sample entity for this workspace!";
        } else {
            title = "Sample information";
            message += "Sample name: " + sample.getName() + "\n";
            message += "Sample ID: " + sample.getId() + "\n";
            message += "Sample path: " + sample.getFilepath() + "\n";
        }
        // need to use text area so you can copy the info to clipboard
        JTextArea textarea= new JTextArea(message);
        textarea.setEditable(false);
        JOptionPane.showMessageDialog(FrameworkImplProvider.getMainFrame(),
                textarea, title, JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public String getActionLabel() {
        return "  Show Sample Info";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj != null && (obj instanceof TmWorkspace);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }
    
    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }
}
