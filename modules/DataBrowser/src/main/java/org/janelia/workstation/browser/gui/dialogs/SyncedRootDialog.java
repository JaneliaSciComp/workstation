package org.janelia.workstation.browser.gui.dialogs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public class SyncedRootDialog extends ModalDialog {

    private JTextField pathTextField;
    private final SubjectComboBox subjectCombobox;
    private SyncedRoot syncedRoot;

    public SyncedRootDialog() {

        setLayout(new BorderLayout());

        GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel();

        attrPanel.addSeparator("Synchronized Folder");

        JLabel instructions = new JLabel(
                "<html><font color='#959595' size='-1'>" +
                        "Specify a folder to search, in Linux path style, e.g. /misc/public<br>" +
                        "If a sample with the same name exits it will be updated. Otherwise a new sample will be created.</font></html>");

        attrPanel.addItem(instructions);

        this.pathTextField = new JTextField(50);
        pathTextField.setToolTipText("The filepath must be accessible to the backend JADE service.");
        attrPanel.addItem("Filepath", pathTextField);

        subjectCombobox = new SubjectComboBox();
        subjectCombobox.setToolTipText("User or group who should own the discovered objects");
        //attrPanel.addItem("Object owner", subjectCombobox);

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close dialog without doing anything");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton okButton = new JButton("OK");
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
        }
        else {
            setTitle("Add Synchronized Folder");
        }

        // Decide which subjects we are allowed to write with
        Map<String, Subject> subjectsByKey = DomainMgr.getDomainMgr().getSubjectsByKey();
        List<Subject> writeableSubjects = AccessManager.getAccessManager().getActualWriterSet()
                .stream().map(subjectsByKey::get).collect(Collectors.toList());
        subjectCombobox.setItems(writeableSubjects, AccessManager.getAccessManager().getActualSubject());

        // Show dialog and wait
        packAndShow();
    }

    private void saveAndClose() {

        String imagesPath = pathTextField.getText();
        final Subject subject = subjectCombobox.getSelectedItem();

        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private String taskDisplayName;

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {
                taskDisplayName = "Synced Root for "+imagesPath;

                setStatus("Submitting task " + taskDisplayName);

                List<String> discoveryAgents = new ArrayList<>();
                discoveryAgents.add("org.janelia.jacs2.asyncservice.files.N5DiscoveryAgent");

                AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
                ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                        .add("-imagesPath", imagesPath)
                        .add("-discoveryAgents")
                        .add(Iterables.toArray(discoveryAgents, String.class));

                if (syncedRoot != null) {
                    serviceArgsBuilder.add("-syncedRootId", syncedRoot.getId().toString());
                }

                if (subject!=null) {
                    serviceArgsBuilder.add("-ownerKey", subject.getKey());
                }

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
        worker.executeWithEvents();

        setVisible(false);
    }
}
