package org.janelia.workstation.admin;

import org.janelia.model.domain.tiledMicroscope.TmOperation;

import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.Refreshable;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author schauderd
 */
public class RetrieveLogsPanel extends JPanel implements Refreshable {
    private static final Logger log = LoggerFactory.getLogger(RetrieveLogsPanel.class);

    private AdministrationTopComponent parent;

    public RetrieveLogsPanel(AdministrationTopComponent parent) {
        this.parent = parent;
        refresh();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        removeAll();
    
        JPanel titlePanel = new TitlePanel("Retrieve Logs", "Return To Top Menu",
                event -> refresh(),
                event -> returnHome());
        add(titlePanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setMaximumSize(new Dimension(
                500,500));

        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 100, 300, 100));
        contentPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;


        final JLabel workspaceNameLabel = new JLabel("Workspace ID (This is required)");
        final JTextField workspaceNameTextField = new JTextField();
        workspaceNameTextField.setText("");
        workspaceNameTextField.setEditable(true);
        workspaceNameTextField.setFocusable(true);

        final JLabel statusLabel = new JLabel("");

        constraints.gridx = 0;
        constraints.gridy = 0;
        contentPanel.add(workspaceNameLabel, constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        contentPanel.add(workspaceNameTextField, constraints);

        final JLabel neuronNameLabel = new JLabel("Neuron ID");
        final JTextField neuronNameTextField = new JTextField();
        neuronNameTextField.setText("");
        neuronNameTextField.setEditable(true);
        neuronNameTextField.setFocusable(true);
        constraints.gridx = 0;
        constraints.gridy = 1;
        contentPanel.add(neuronNameLabel, constraints);
        constraints.gridx = 1;
        constraints.gridy = 1;
        contentPanel.add(neuronNameTextField, constraints);

        File defaultFile = Utils.getOutputFile(System.getProperty("user.home"), "log_results", "csv");
        final JTextField fileOutputLocation = new JTextField();
        fileOutputLocation.setText(defaultFile.getAbsolutePath());
        JButton outputButton = new JButton("Choose Export Location");
        constraints.gridx = 1;
        constraints.gridy = 2;
        contentPanel.add(fileOutputLocation, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        contentPanel.add(outputButton, constraints);

        add(contentPanel, BorderLayout.CENTER);
        // Add an ActionListener to the button
        outputButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create a JFileChooser
                JFileChooser fileChooser = new JFileChooser();
                File defaultFile = Utils.getOutputFile(System.getProperty("user.home"), "log_results", "csv");
                fileChooser.setSelectedFile(defaultFile);
                // Show the file chooser dialog
                int result = fileChooser.showOpenDialog(FrameworkAccess.getMainFrame());

                // Check if the user selected a file
                if (result == JFileChooser.APPROVE_OPTION) {
                    // Get the selected file
                    java.io.File selectedFile = fileChooser.getSelectedFile();
                    fileOutputLocation.setText(selectedFile.getAbsolutePath());
                    // Print the file path
                    System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                }
            }
        });
        JButton okButton = new JButton("Get Logs");
        okButton.setToolTipText("Get Logs from the Server");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                TiledMicroscopeDomainMgr domainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
                String workspaceId = workspaceNameTextField.getText();
                String neuronId = neuronNameTextField.getText();
                Long workspaceId_L =(workspaceId.equals("")?null:Long.parseLong(workspaceId));
                Long neuronId_L =(neuronId.equals("")?null:Long.parseLong(neuronId));
                List<TmOperation> logs = domainMgr.getOperationLogs(workspaceId_L,
                        neuronId_L,
                        null, null, AccessManager.getSubjectKey());
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(fileOutputLocation.getText()));
                    for (TmOperation log : logs) {
                        writer.write(
                                Objects.toString(log.getTimestamp(), "") + "," +
                                        Objects.toString(log.getWorkspaceId(), "") + "," +
                                        Objects.toString(log.getNeuronId(), "") + "," +
                                        Objects.toString(log.getUser(), "") + "," +
                                        Objects.toString(log.getActivity(), "") + "," +
                                        Objects.toString(log.getElapsedTime(), "")
                        );
                        writer.newLine();
                    }
                    writer.close();
                    statusLabel.setText("Finished downloading logs");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 3;
        contentPanel.add(okButton, constraints);

        constraints.gridx = 1;
        constraints.gridy = 4;
        contentPanel.add(statusLabel, constraints);
        revalidate();
    }

    @Override
    public void refresh() {
        setupUI();
    }

    private void returnHome() {
        parent.viewTopMenu();
    }
}
