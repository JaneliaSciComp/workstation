package org.janelia.it.workstation.gui.large_volume_viewer.launch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.awt.BorderLayout;
import java.awt.Color;
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

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.integration.framework.domain.ObjectOpenAcceptor;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeRestClient;
import org.janelia.it.workstation.gui.large_volume_viewer.components.PathCorrectionKeyListener;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.EditWorkspaceNameDialog;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with a known sample, to create a tiled microscope workspace, and load it with SWC input.
 * 
 * @author fosterl
 */
@ServiceProvider(service = ObjectOpenAcceptor.class, path = ObjectOpenAcceptor.LOOKUP_PATH)
public class LoadedWorkspaceCreator implements ObjectOpenAcceptor {
    
    private static final Logger LOG = LoggerFactory.getLogger(LoadedWorkspaceCreator.class);

    private static final int MENU_ORDER = 500;
   
    @Override
    public void acceptObject(Object obj) {
        DomainObject domainObject = (DomainObject) obj;
        TmSample sample = (TmSample) domainObject;
        JFrame mainFrame = FrameworkImplProvider.getMainFrame();

        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog();
        String workspaceName = dialog.showForSample(sample);
        if (workspaceName==null) {
            LOG.info("Aborting workspace creation: no valid name was provided by the user");
            return;
        }

        final JDialog inputDialog = new JDialog(mainFrame, true);
        final JTextField pathTextField = new JTextField();
        final JCheckBox systemOwnerCheckbox = new JCheckBox();
        final JLabel errorLabel = new JLabel("   ");
        errorLabel.setForeground(Color.red);
        pathTextField.addKeyListener(new PathCorrectionKeyListener(pathTextField));
        pathTextField.setToolTipText("Backslashes will be converted to /.");
        final JLabel workspaceNameLabel = new JLabel("Workspace Name");
        final JTextField workspaceNameTextField = new JTextField();
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
        inputDialog.add(errorLabel);

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Send path to linux.");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                errorLabel.setText("");
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
                if (! cf.isServerPathAvailable(swcFolder, true) ) {
                    errorLabel.setText("'" + swcFolder + "' not found on server. Please Try again.");
                } else {
                    inputDialog.setVisible(false);
                    importSWC(sample.getId(), workspaceNameTextField.getText().trim(), swcFolder, systemOwnerCheckbox.isSelected());
                }
            }
        });
        buttonPanel.add(okButton, SystemInfo.isMac ? BorderLayout.LINE_END : BorderLayout.LINE_START);

        inputDialog.setSize(500, 280);
        inputDialog.setLocationRelativeTo(mainFrame);
        inputDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        inputDialog.setVisible(true);
    }

    private void importSWC(Long sampleId, String workspace, String swcFolder, boolean withSystemOwner) {
        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private String taskDisplayName;

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {
                String taskName = new File(swcFolder).getName();
                taskDisplayName = taskName + " for 3D tiled microscope sample " + sampleId;

                setStatus("Submitting task " + taskDisplayName);

                Long taskId = startImportSWC(sampleId, workspace, swcFolder, withSystemOwner);

                setServiceId(taskId);

                // Wait until task is finished
                super.doStuff();

                if (isCancelled()) throw new CancellationException();
                setStatus("Done importing");
            }

        };
        worker.executeWithEvents();
    }

    private Long startImportSWC(Long sampleId, String workspace, String swcFolder, boolean withSystemOwner) {
        AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
        ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                .add("-sampleId", sampleId.toString());
        serviceArgsBuilder.add("-workspace", workspace);
        serviceArgsBuilder.add("-swcDirName", swcFolder);
        if (withSystemOwner) {
            serviceArgsBuilder.add("-withSystemOwner");
        }
        return asyncServiceClient.invokeService("swcImport",
                serviceArgsBuilder.build(),
                null,
                ImmutableMap.of()
        );
    }

    @Override
    public String getActionLabel() {
        return "  Load Linux SWC Folder into New Workspace on Sample";
    }

    @Override
    public boolean isCompatible(Object obj) {
        return obj != null && (obj instanceof TmSample);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }
    
    @Override
    public Integer getOrder() {
        return MENU_ORDER;
    }

    @Override
    public boolean isPrecededBySeparator() {
        return false;
    }

    @Override
    public boolean isSucceededBySeparator() {
        return false;
    }

}
