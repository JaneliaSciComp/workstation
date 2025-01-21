package org.janelia.workstation.browser.gui.dialogs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.files.DiscoveryAgentType;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.model.security.Subject;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.SubjectComboBox;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public class SyncedRootDialog extends ModalDialog {

    private JTextField pathTextField;
    private JLabel accessKeyLabel;
    private JTextField accessKeyTextField;
    private JLabel secretKeyLabel;
    private JTextField secretKeyTextField;
    private JTextField nameField;
    private JTextField depthField;
    private JPanel subjectPanel;
    private SubjectComboBox subjectCombobox;
    private JTextField subjectTextField;
    private SyncedRoot syncedRoot;
    private HashMap<DiscoveryAgentType, JCheckBox> agentTypeMap = new LinkedHashMap<>();


    public SyncedRootDialog() {

        setLayout(new BorderLayout());

        GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel();

        attrPanel.addSeparator("Synchronized Folder");

        JLabel instructions = new JLabel(
                "<html><font color='#959595' size='-1'>" +
                        "Specify a path to search, in Linux path style e.g. /misc/public<br>" +
                        "</font></html>");

        attrPanel.addItem(instructions);

        pathTextField = new JTextField(50);
        pathTextField.setToolTipText("The filepath must be accessible to the backend JADE service");
        attrPanel.addItem("Path", pathTextField);

        JCheckBox storageCredentialsRequiredCheckbox = new JCheckBox();
        attrPanel.addItem("Storage requires credentials", storageCredentialsRequiredCheckbox);

        accessKeyTextField = new JTextField(50);
        accessKeyTextField.setToolTipText("Access key for the provided path");
        accessKeyLabel = attrPanel.addItem("Access Key", accessKeyTextField);
        accessKeyLabel.setVisible(false);
        accessKeyTextField.setVisible(false);

        secretKeyTextField = new JTextField(50);
        secretKeyTextField.setToolTipText("Secret key for the provided path");
        secretKeyLabel = attrPanel.addItem("Secret Key", secretKeyTextField);
        secretKeyLabel.setVisible(false);
        secretKeyTextField.setVisible(false);

        storageCredentialsRequiredCheckbox.addActionListener(e -> {
            if (storageCredentialsRequiredCheckbox.isSelected()) {
                accessKeyLabel.setVisible(true);
                secretKeyLabel.setVisible(true);
                accessKeyTextField.setVisible(true);
                secretKeyTextField.setVisible(true);
            } else {
                accessKeyLabel.setVisible(false);
                secretKeyLabel.setVisible(false);
                accessKeyTextField.setVisible(false);
                secretKeyTextField.setVisible(false);
            }
        });

        this.nameField = new JTextField(50);
        nameField.setToolTipText("Name of the Synchronized Folder in the Workstation. If blank, the filepath will be used.");
        attrPanel.addItem("Name (optional)", nameField);

        this.depthField = new JTextField(20);
        depthField.setToolTipText("Depth of folders to traverse when discovering files");
        depthField.setText("2");
        attrPanel.addItem("Depth", depthField);

        subjectCombobox = new SubjectComboBox();
        subjectCombobox.setToolTipText("User or group who should own the samples");

        this.subjectTextField = new JTextField(20);
        subjectTextField.setEditable(false);
        subjectTextField.setToolTipText("Owner cannot be changed after creation");

        this.subjectPanel = new JPanel();
        attrPanel.addItem("Owner", subjectPanel);

        final JPanel agentPanel = new JPanel();
        agentPanel.setLayout(new BoxLayout(agentPanel, BoxLayout.PAGE_AXIS));

        for (DiscoveryAgentType value : DiscoveryAgentType.values()) {
            JCheckBox checkbox = new JCheckBox(value.getLabel());
            agentPanel.add(checkbox);
            agentTypeMap.put(value, checkbox);
        }

        attrPanel.addItem("Discover", agentPanel);

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close dialog without doing anything");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Save the changes and close the dialog");
        saveButton.addActionListener(e -> saveAndClose(false));

        JButton okButton = new JButton("Save and Synchronize");
        okButton.setToolTipText("Save the changes and begin data synchronization");
        okButton.addActionListener(e -> saveAndClose(true));

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(saveButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showDialog() {
        showDialog(null);
    }

    public void showDialog(SyncedRoot syncedRoot) {

        this.syncedRoot = syncedRoot;

        // Decide which subjects we are allowed to write with
        Map<String, Subject> subjectsByKey = DomainMgr.getDomainMgr().getSubjectsByKey();
        List<Subject> writeableSubjects = AccessManager.getAccessManager().getActualWriterSet()
                .stream().map(subjectsByKey::get).collect(Collectors.toList());
        subjectCombobox.setItems(writeableSubjects, AccessManager.getAccessManager().getActualSubject());

        if (syncedRoot != null) {
            setTitle("Edit Synchronized Folder");
            pathTextField.setText(syncedRoot.getFilepath());
            nameField.setText(syncedRoot.getName());
            depthField.setText(syncedRoot.getDepth()+"");

            subjectTextField.setText(syncedRoot.getOwnerKey());
            subjectPanel.add(subjectTextField);

            for (DiscoveryAgentType agentType : DiscoveryAgentType.values()) {
                JCheckBox checkBox = agentTypeMap.get(agentType);
                checkBox.setSelected(syncedRoot.getDiscoveryAgents().contains(agentType));
            }
        } else {
            setTitle("Add Synchronized Folder");
            subjectPanel.add(subjectCombobox);
        }

        // Show dialog and wait
        packAndShow();
    }

    private void saveAndClose(boolean sync) {

        if (syncedRoot == null) {
            syncedRoot = new SyncedRoot();
        }

        syncedRoot.setFilepath(pathTextField.getText());

        if (StringUtils.isBlank(syncedRoot.getFilepath())) {
            JOptionPane.showMessageDialog(this, "You must enter a filepath to continue", "No filepath given", JOptionPane.ERROR_MESSAGE);
            return;
        }

        syncedRoot.setName(StringUtils.isBlank(nameField.getText()) ? syncedRoot.getFilepath() : nameField.getText());

        if (syncedRoot.getId() == null) {
            // Creating new object, who should own it?
            final Subject subject = subjectCombobox.getSelectedItem();
            if (subject != null) {
                syncedRoot.setOwnerKey(subject.getKey());
            }
        }

        try {
            syncedRoot.setDepth(Integer.parseInt(depthField.getText()));
            if (syncedRoot.getDepth() < 1 || syncedRoot.getDepth() > 20) throw new NumberFormatException();
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a number between 1 and 20 for depth", "Invalid depth", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (DiscoveryAgentType agentType : DiscoveryAgentType.values()) {
            JCheckBox checkBox = agentTypeMap.get(agentType);
            if (checkBox.isSelected()) {
                syncedRoot.getDiscoveryAgents().add(agentType);
            }
            else {
                syncedRoot.getDiscoveryAgents().remove(agentType);
            }
        }

        if (StringUtils.isNotBlank(accessKeyTextField.getText())) {
            syncedRoot.setStorageAttribute("AccessKey", accessKeyTextField.getText().trim());
        }
        if (StringUtils.isNotBlank(secretKeyTextField.getText())) {
            syncedRoot.setStorageAttribute("SecretKey", secretKeyTextField.getText().trim());
        }

        if (syncedRoot.getDiscoveryAgents().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select one or more discovery agents", "No agents selected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (sync) {
            refreshSyncedRoot(syncedRoot);
        }

        setVisible(false);
    }

    private static void refreshSyncedRoot(SyncedRoot root) {

        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private String taskDisplayName = "Synchronizing Folder "+root.getName();

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {

                SyncedRoot savedRoot = DomainMgr.getDomainMgr().getModel().save(root);

                setStatus("Submitting task " + taskDisplayName);
                AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
                ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                        .add("-syncedRootId", savedRoot.getId().toString());

                Long taskId = asyncServiceClient.invokeService("syncedRoot",
                        serviceArgsBuilder.build(),
                        null,
                        ImmutableMap.of(),
                        savedRoot.getStorageAttributes()
                );

                setServiceId(taskId);

                // Wait until task is finished
                super.doStuff();

                if (isCancelled()) throw new CancellationException();
                setStatus("Done");
            }

        };
        worker.setSuccessCallback(() -> {
            SimpleWorker.runInBackground(() -> DomainMgr.getDomainMgr().getModel().invalidateAll());
            return null;
        });
        worker.executeWithEvents();
    }
}
