package org.janelia.it.workstation.gui.framework.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;

/**
 * right-click on workspace in Data Explorer, get info on its sample
 */
public class ShowLVVWorkspaceInfoActionListener implements ActionListener {
    private RootedEntity workspaceEntity;

    public ShowLVVWorkspaceInfoActionListener(RootedEntity workspaceEntity) {
        this.workspaceEntity = workspaceEntity;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String sampleID = workspaceEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
        Entity sampleEntity = null;
        try {
            sampleEntity = ModelMgr.getModelMgr().getEntityById(sampleID);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        String title;
        String message = "Workspace name: " + workspaceEntity.getName() + "\n";
        if (sampleEntity == null) {
            title = "Error";
            message += "\nCould not retrieve sample entity for this workspace!";
        } else {
            title = "Sample information";
            String path = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            message += "Sample name: " + sampleEntity.getName() + "\n";
            message += "Sample ID: " + sampleID + "\n";
            message += "Sample path: " + path + "\n";
        }
        // need to use text area so you can copy the info to clipboard
        JTextArea textarea= new JTextArea(message);
        textarea.setEditable(false);
        JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                textarea, title, JOptionPane.PLAIN_MESSAGE);
    }
}
