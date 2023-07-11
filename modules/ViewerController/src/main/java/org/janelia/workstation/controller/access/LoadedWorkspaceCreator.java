package org.janelia.workstation.controller.access;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.CancellationException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.controller.action.PathCorrectionKeyListener;
import org.janelia.workstation.controller.dialog.EditWorkspaceNameDialog;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with a known sample, to create a tiled microscope workspace, and load it with SWC input.
 * 
 * @author fosterl
 */
@ActionID(
        category = "actions",
        id = "LoadedWorkspaceCreator"
)
@ActionRegistration(
        displayName = "#CTL_LoadedWorkspaceCreator",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Horta", position = 1540)
})
@NbBundle.Messages("CTL_LoadedWorkspaceCreator=Load Linux SWC Folder into New Workspace on Sample")
public class LoadedWorkspaceCreator extends BaseContextualNodeAction {
    
    private static final Logger LOG = LoggerFactory.getLogger(LoadedWorkspaceCreator.class);

    private TmSample sample;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(TmSample.class)) {
            sample = getNodeContext().getSingleObjectOfType(TmSample.class);
            setEnabledAndVisible(true);
        }
        else {
            sample = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        TmSample sample = this.sample;

        JFrame mainFrame = FrameworkAccess.getMainFrame();

        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog();
        String workspaceName = dialog.showForSample(sample);
        if (workspaceName==null) {
            LOG.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        final JDialog inputDialog = new JDialog(mainFrame, true);
        final JTextField pathTextField = new JTextField();
        final JCheckBox systemOwnerCheckbox = new JCheckBox();
        pathTextField.addKeyListener(new PathCorrectionKeyListener(pathTextField));
        pathTextField.setToolTipText("Backslashes will be converted to /.");
        final JLabel workspaceNameLabel = new JLabel("Workspace Name");
        final JTextField workspaceNameTextField = new JTextField();
        workspaceNameTextField.setText(workspaceName);
        workspaceNameTextField.setEditable(false);
        workspaceNameTextField.setFocusable(false);
        final JCheckBox markAsFragmentsCheckbox = new JCheckBox();
        workspaceNameTextField.setText(workspaceName);
        workspaceNameTextField.setEditable(false);
        workspaceNameTextField.setFocusable(false);
        inputDialog.setTitle("SWC Load-to-Workspace Parameters");
        inputDialog.setLayout(new GridLayout(6, 1));
        inputDialog.add(workspaceNameLabel);
        inputDialog.add(workspaceNameTextField);
        inputDialog.add(new JLabel("Enter full path to input folder"));
        inputDialog.add(pathTextField);
        inputDialog.add(new JLabel("Assign all neurons to mouselight"));
        inputDialog.add(systemOwnerCheckbox);
        inputDialog.add(new JLabel("Mark all neurons as fragments"));
        inputDialog.add(markAsFragmentsCheckbox);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                inputDialog.setVisible(false);
            }
        });
        buttonPanel.add(cancelButton, SystemInfo.isMac ? BorderLayout.LINE_START : BorderLayout.LINE_END);
        inputDialog.add(buttonPanel);

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Send path to linux.");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String temp = pathTextField.getText().trim().replace("\\", "/");
                pathTextField.setText(temp); // Show user what we try
                StringBuilder bldr = new StringBuilder();
                for (int i = 0; i < temp.length(); i++) {
                    final char nextChar = temp.charAt(i);
                    switch (nextChar) {
                        case '\\':
                            bldr.append('/');
                            break;
                        case '\n':
                        case '\r':
                            break;
                        default :
                            // Most characters are just fine.
                            bldr.append(nextChar);
                            break;
                    }
                }
                TiledMicroscopeRestClient cf = new TiledMicroscopeRestClient();
                String swcFolder = bldr.toString().trim();

                if (cf.isServerPathAvailable(swcFolder, true) ) {
                    Boolean markAsFragments;
                    inputDialog.setVisible(false);
                    String neuronsOwnerKey;
                    if (systemOwnerCheckbox.isSelected()) {
                        neuronsOwnerKey = ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
                    } else {
                        neuronsOwnerKey = null;
                    }
                    if (markAsFragmentsCheckbox.isSelected()) {
                        markAsFragments = true;
                    } else {
                        markAsFragments = false;
                    }

                    importSWC(sample.getId(), workspaceNameTextField.getText().trim(), swcFolder, neuronsOwnerKey,
                            markAsFragments);
                }
            }
        });
        buttonPanel.add(okButton, SystemInfo.isMac ? BorderLayout.LINE_END : BorderLayout.LINE_START);

        inputDialog.setSize(500, 280);
        inputDialog.setLocationRelativeTo(mainFrame);
        inputDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        inputDialog.setVisible(true);
    }

    private void importSWC(Long sampleId, String workspace, String swcFolder, String neuronsOwner, Boolean markAsFragments) {
        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private String taskDisplayName;

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {
                String taskName = new File(swcFolder).getName();
                taskDisplayName = taskName + " for Horta Sample " + sampleId;

                setStatus("Submitting task " + taskDisplayName);

                Long taskId = startImportSWC(sampleId, workspace, swcFolder, neuronsOwner, markAsFragments);

                setServiceId(taskId);

                // Wait until task is finished
                super.doStuff();

                if (isCancelled()) throw new CancellationException();
                setStatus("Done importing");
            }

        };
        worker.executeWithEvents();
    }

    private Long startImportSWC(Long sampleId, String workspace, String swcFolder, String neuronsOwner,
                                Boolean markAsFragments) {
        AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
        ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                .add("-sampleId", sampleId.toString());
        serviceArgsBuilder.add("-workspace", workspace);
        serviceArgsBuilder.add("-swcDirName", swcFolder);
        serviceArgsBuilder.add("-markAsFragments", markAsFragments.toString());
        if (StringUtils.isNotBlank(neuronsOwner)) {
            serviceArgsBuilder.add("-neuronsOwner", neuronsOwner);
        }
        return asyncServiceClient.invokeService("swcImport",
                serviceArgsBuilder.build(),
                null,
                ImmutableMap.of()
        );
    }

}
