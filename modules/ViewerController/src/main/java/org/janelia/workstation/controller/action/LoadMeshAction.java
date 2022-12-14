package org.janelia.workstation.controller.action;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

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

    JTextField locationField;
    public LoadMeshAction() {
        super("Load Object Mesh");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String locationText = "";
        String meshNameText = "";
        JPanel meshPanel = new JPanel();
        meshPanel.setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        meshPanel.add(new JLabel("Mesh Location"), gbc);

        gbc.gridx = 1;
        locationField = new JTextField(locationText, 20);
        meshPanel.add(locationField, gbc);

        gbc.gridx = 2;
        JButton meshChooser = new JButton("Choose Mesh");
        meshChooser.addActionListener(ev -> chooseMesh());
        meshPanel.add(meshChooser, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        meshPanel.add(new JLabel("Mesh Name"), gbc);

        gbc.gridx = 1;
        final JTextField nameField = new JTextField(meshNameText, 20);
        meshPanel.add(nameField, gbc);

        int result = JOptionPane.showConfirmDialog(null, meshPanel,
                "Enter Object Mesh ", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            saveObjectMesh(nameField.getText(), locationField.getText());
        }
    }

    public void chooseMesh() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Object Mesh Files", "obj"));
        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            locationField.setText(fileChooser.getSelectedFile().getAbsolutePath());
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
