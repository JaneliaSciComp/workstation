package org.janelia.workstation.controller.access;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.model.security.Subject;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.common.gui.support.SubjectComboBox;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

@ActionID(
        category = "Services",
        id = "HortaDataSync"
)
@ActionRegistration(
        displayName = "#CTL_LoadTiledMicroscopeData",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Services", position = 1500, separatorBefore = 1499)
})
@NbBundle.Messages("CTL_LoadTiledMicroscopeData=Load Horta Data")
public class HortaDataSync extends CallableSystemAction {

    @Override
    public String getName() {
        return "Load Horta Data";
    }

    @Override
    protected String iconResource() {
        return "images/folder_database.png";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public void performAction() {
        HortaDataSyncDialog hortaDataSyncDialog = new HortaDataSyncDialog();
        hortaDataSyncDialog.showDialog();
    }

    private static class HortaDataSyncDialog extends ModalDialog {

        private JTextField pathTextField;
        private final SubjectComboBox subjectCombobox;

        public HortaDataSyncDialog() {

            setTitle("Synchronize Horta Data");
            setLayout(new BorderLayout());

            GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel();

            attrPanel.addSeparator("Synchronize Horta Data");

            JLabel instructions = new JLabel(
                    "Choose a folder to search for TM samples. Each sample folder should contain a transform.txt.\n" +
                    "Samples will be synchronized with the TM samples owned by the given user. If a sample with the same\n" +
                    "name exits, it will be updated. Otherwise a new sample will be created.");

            attrPanel.addItem(instructions);

            this.pathTextField = new JTextField(60);
            pathTextField.setToolTipText("The filepath must be accessible to the backend JADE service.\n" +
                    "Each child folder represents a TM Sample if it contains a transform.txt file");
            attrPanel.addItem("Filepath containing sample imagery folders", pathTextField);

            subjectCombobox = new SubjectComboBox();
            subjectCombobox.setToolTipText("User or group who should own the samples");
            attrPanel.addItem("Sample owner", subjectCombobox);

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
                    taskDisplayName = "Horta File Sync for "+imagesPath;

                    setStatus("Submitting task " + taskDisplayName);

                    AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
                    ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                            .add("-imagesPath", imagesPath);
                    serviceArgsBuilder.add("-ownerKey", subject.getKey());

                    Long taskId = asyncServiceClient.invokeService("hortaDataSync",
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
}
