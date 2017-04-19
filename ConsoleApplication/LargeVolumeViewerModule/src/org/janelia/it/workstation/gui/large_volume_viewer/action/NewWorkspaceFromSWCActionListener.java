package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.SwcImportTask;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.api.facade.interfaces.LegacyFacade;
import org.janelia.it.workstation.browser.util.SystemInfo;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.components.PathCorrectionKeyListener;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.EditWorkspaceNameDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a workspace on top of an existing TM Sample and populates it with neurons from a
 * folder of SWC files found on the server-side. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewWorkspaceFromSWCActionListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(NewWorkspaceFromSWCActionListener.class);
    
    private TmSample sample;

    public NewWorkspaceFromSWCActionListener(TmSample sample) {
        this.sample = sample;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewDirectedSessionActionListener.actionPerformed");

        final JFrame mainFrame = ConsoleApp.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private String userInput;
            private Task task;
            
            @Override
            protected void doStuff() throws Exception {

                EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog("Workspace Name");
                String workspaceName = dialog.showForSample(sample);
                if (workspaceName==null) {
                    log.info("Aborting workspace creation: no valid name was provided by the user");
                    return;
                }
                                
                final JDialog inputDialog = new JDialog(mainFrame, true);
                final JTextField pathTextField = new JTextField();
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
                inputDialog.add(new JLabel("Enter Full Path to Input Folder"));
                inputDialog.add(pathTextField);
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new BorderLayout());
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        userInput = null;
                        inputDialog.setVisible(false);
                    }
                });
                buttonPanel.add(cancelButton, SystemInfo.isMac ? BorderLayout.LINE_START : BorderLayout.LINE_END);
                inputDialog.add(buttonPanel);
                inputDialog.add(errorLabel);
                final LegacyFacade cf = DomainMgr.getDomainMgr().getLegacyFacade();

                JButton okButton = new JButton("OK");
                okButton.setToolTipText("Send path to linux.");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        errorLabel.setText("");
                        String temp = pathTextField.getText().trim();
                        temp = temp.replace("\\", "/");
                        pathTextField.setText(temp); //Show user what we try
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
                        userInput = bldr.toString().trim();
                        if (! cf.isServerPathAvailable(userInput, true) ) {
                            errorLabel.setText("'" + userInput + "' not found on server. Please Try again.");
                        }
                        else {
                            inputDialog.setVisible(false);
                        }
                    }
                });
                buttonPanel.add(okButton, SystemInfo.isMac ? BorderLayout.LINE_END : BorderLayout.LINE_START);
                
                inputDialog.setSize(500, 280);
                inputDialog.setLocationRelativeTo(mainFrame);
                inputDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                inputDialog.setVisible(true);
                
                workspaceName = workspaceNameTextField.getText().trim();

                log.info("Processing " + userInput);
                
                if (userInput != null) {
                    String ownerKey = AccessManager.getSubjectKey();
                    // Expect the sample to be the 'main entity' of the LVV, since there is
                    // no workspace.  
                    Long sampleId = sample.getId();
                    HashSet<TaskParameter> taskParameters = new HashSet<>();
                    taskParameters.add(new TaskParameter(SwcImportTask.PARAM_sampleId, sampleId.toString(), null));
                    taskParameters.add(new TaskParameter(SwcImportTask.PARAM_userName, ownerKey, null));
                    taskParameters.add(new TaskParameter(SwcImportTask.PARAM_workspaceName, workspaceName, null));
                    taskParameters.add(new TaskParameter(SwcImportTask.PARAM_topLevelFolderName, userInput, null));

                    String taskName = new File(userInput).getName();
                    String displayName = taskName + " for 3D tiled microscope sample " + sampleId;
                    task = StateMgr.getStateMgr().submitJob(SwcImportTask.PROCESS_NAME, displayName, taskParameters);
                }
            }                         

            @Override
            protected void hadSuccess() {
                if (task!=null) {
                    // Launch another thread/worker to monitor the 
                    // remote-running task.
                    TaskMonitoringWorker tmw = new TaskMonitoringWorker(task.getObjectId()) {
                        @Override
                        public String getName() {
                            File uiFile = new File(userInput);
                            return "Importing all SWCs in " + uiFile.getName();
                        }
                        
                    };
                    tmw.executeWithEvents();
                }
            }
            
            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        worker.execute();
    }
}
