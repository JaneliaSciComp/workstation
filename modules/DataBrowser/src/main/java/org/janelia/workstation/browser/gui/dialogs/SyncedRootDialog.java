package org.janelia.workstation.browser.gui.dialogs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.model.domain.files.DiscoveryAgentType;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;

public class SyncedRootDialog extends ModalDialog {

    private JTextField nameField;
    private JTextField pathTextField;
    private JTextField depthField;
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

        this.nameField = new JTextField(50);
        nameField.setToolTipText("Name of the Synchronized Folder in the Workstation");
        attrPanel.addItem("Name", nameField);

        this.pathTextField = new JTextField(50);
        pathTextField.setToolTipText("The filepath must be accessible to the backend JADE service");
        attrPanel.addItem("Path", pathTextField);

        this.depthField = new JTextField(20);
        depthField.setToolTipText("Depth of folders to traverse when discovering files");
        depthField.setText("2");
        attrPanel.addItem("Depth", depthField);

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

        JButton okButton = new JButton("Save and Synchronize");
        okButton.setToolTipText("Close dialog and begin data synchronization");
        okButton.addActionListener(e -> saveAndClose());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showDialog() {
        showDialog(null);
    }

    public void showDialog(SyncedRoot syncedRoot) {

        this.syncedRoot = syncedRoot;

        if (syncedRoot != null) {
            setTitle("Edit Synchronized Folder");
            pathTextField.setText(syncedRoot.getFilepath());
            nameField.setText(syncedRoot.getName());
            depthField.setText(syncedRoot.getDepth()+"");

            for (DiscoveryAgentType agentType : DiscoveryAgentType.values()) {
                JCheckBox checkBox = agentTypeMap.get(agentType);
                checkBox.setSelected(syncedRoot.getDiscoveryAgents().contains(agentType));
            }
        }
        else {
            setTitle("Add Synchronized Folder");
        }

        // Show dialog and wait
        packAndShow();
    }

    private void saveAndClose() {

        if (syncedRoot == null) {
            syncedRoot = new SyncedRoot();
        }

        syncedRoot.setName(nameField.getText());
        syncedRoot.setFilepath(pathTextField.getText());
        syncedRoot.setDepth(Integer.parseInt(depthField.getText()));

        for (DiscoveryAgentType agentType : DiscoveryAgentType.values()) {
            JCheckBox checkBox = agentTypeMap.get(agentType);
            if (checkBox.isSelected()) {
                syncedRoot.getDiscoveryAgents().add(agentType);
            }
            else {
                syncedRoot.getDiscoveryAgents().remove(agentType);
            }
        }

        refreshSyncedRoot(syncedRoot);

        setVisible(false);
    }

    public static void refreshSyncedRoot(SyncedRoot root) {

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
                        ImmutableMap.of()
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
