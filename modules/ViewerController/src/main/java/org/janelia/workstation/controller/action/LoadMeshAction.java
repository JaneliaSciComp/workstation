package org.janelia.workstation.controller.action;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.*;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.workstation.controller.ViewerEventBus;
import org.janelia.workstation.controller.eventbus.MeshCreateEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.LoadMeshAction"
)
@ActionRegistration(
        displayName = "#CTL_LoadMeshAction",
        lazy = true
)
@Messages("CTL_LoadMeshAction=Load Object Mesh")
public final class LoadMeshAction
        extends AbstractAction
        implements ActionListener
{
    public LoadMeshAction() {
        super("Load Object Mesh");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String locationText = "";
        String meshNameText = "";
        JPanel meshPanel = new JPanel();
        meshPanel.setLayout(new GridLayout(2, 2));
        meshPanel.add(new JLabel("Mesh Location:"));
        final JTextField locationField = new JTextField(locationText, 40);
        meshPanel.add(locationField);
        meshPanel.add(new JLabel("Mesh Name"));
        final JTextField nameField = new JTextField(meshNameText, 30);
        meshPanel.add(nameField);

        int result = JOptionPane.showConfirmDialog(null, meshPanel,
                "Enter Object Mesh Values", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            saveObjectMesh(nameField.getText(), locationField.getText());
        }
    }

    public void saveObjectMesh (String meshName, String filename) {
        TmObjectMesh newObjMesh = new TmObjectMesh(meshName, filename);
        try {
            TmModelManager.getInstance().getCurrentWorkspace().addObjectMesh(newObjMesh);
            TmModelManager.getInstance().saveCurrentWorkspace();

            // fire off event for scene editor
            MeshCreateEvent meshEvent = new MeshCreateEvent(this,
                    Arrays.asList(new TmObjectMesh[]{newObjMesh}));
            ViewerEventBus.postEvent(meshEvent);
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }
}
