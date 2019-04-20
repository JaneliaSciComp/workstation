package org.janelia.workstation.gui.large_volume_viewer.launch;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.SimpleActionBuilder;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * right-click on workspace in Data Explorer, get info on its sample
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=1530)
public class ShowWorkspaceInfo extends SimpleActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ShowWorkspaceInfo.class);

    @Override
    protected String getName() {
        return "Show Sample Info";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof TmWorkspace;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return true;
    }

    @Override
    protected void performAction(Object obj) {
    	
    	TmWorkspace workspace = (TmWorkspace)obj;
    	
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
        JTextArea textarea = new JTextArea(message);
        textarea.setEditable(false);
        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                textarea, title, JOptionPane.PLAIN_MESSAGE);
    }
}
