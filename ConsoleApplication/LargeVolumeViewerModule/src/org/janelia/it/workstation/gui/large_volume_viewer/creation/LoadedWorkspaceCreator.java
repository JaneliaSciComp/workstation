package org.janelia.it.workstation.gui.large_volume_viewer.creation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.HashSet;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.nb_action.EntityWrapperCreator;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.SwcImportTask;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.gui.large_volume_viewer.components.PathCorrectionKeyListener;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this with a known sample, to create a tiled microscope workspace, and load it with
 * SWC input.
 * 
 * @author fosterl
 */
@ServiceProvider(service=EntityWrapperCreator.class,path=EntityWrapperCreator.LOOKUP_PATH)
public class LoadedWorkspaceCreator implements EntityWrapperCreator {
    
    private static final Logger log = LoggerFactory.getLogger(LoadedWorkspaceCreator.class);
    
    private RootedEntity rootedEntity;
    
    public void execute() {

        final JFrame mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            private String userInput;
            
            @Override
            protected void doStuff() throws Exception {
                // Simple dialog: just enter the path.  Should be a server-known path.
                final JDialog inputDialog = new JDialog(mainFrame, true);
                final JTextField pathTextField = new JTextField();
                final JLabel errorLabel = new JLabel("   ");
                errorLabel.setForeground(Color.red);
                pathTextField.addKeyListener(new PathCorrectionKeyListener(pathTextField));
                pathTextField.setToolTipText("Backslashes will be converted to /.");
                final JLabel workspaceNameLabel = new JLabel("Workspace Name");
                final JTextField workspaceNameTextField = new JTextField();
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
                final ComputeFacade cf = FacadeManager.getFacadeManager().getComputeFacade();

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
                            errorLabel.setText(userInput + " not found on server. Please Try again.");
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
                
                log.info("Processing " + userInput);
                                
                if (userInput != null) {
                    String ownerKey = SessionMgr.getSessionMgr().getSubject().getKey();
                    // Expect the sample to be the 'main entity' of the LVV, since there is
                    // no workspace.                                                        
                    try {
                        Long sampleId = rootedEntity.getEntityId();
                        HashSet<TaskParameter> taskParameters = new HashSet<>();
                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_sampleId, sampleId.toString(), null));
                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_userName, ownerKey, null));
                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_workspaceName, workspaceNameTextField.getText().trim(), null));
                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_topLevelFolderName, userInput, null));

                        String taskName = new File(userInput).getName();
                        String displayName = taskName + " for 3D tiled microscope sample " + sampleId;
                        final Task task = ModelMgr.getModelMgr().submitJob(SwcImportTask.PROCESS_NAME, displayName, taskParameters);

                        // Launch another thread/worker to monitor the 
                        // remote-running task.
                        TaskMonitoringWorker tmw = new TaskMonitoringWorker(task.getObjectId()) {
                            @Override
                            public void doStuff() throws Exception {
                                super.doStuff();
                            }
                            @Override
                            public String getName() {
                                if (userInput != null) {
                                    File uiFile = new File(userInput);
                                    return "import all SWCs in " + uiFile.getName();
                                } else {
                                    return "import SWC for sample";
                                }
                            }
                            
                        };
                        tmw.executeWithEvents();
                        
                    } catch (Exception e) {
                        String errorString = "Error launching : " + e.getMessage();
                        log.error(errorString);
                        throw e;
                    }
                }
            }                         

            @Override
            protected void hadSuccess() {
            }
            
            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };
        worker.execute();
    }

    @Override
    public void wrapEntity(RootedEntity e) {
        this.rootedEntity = (RootedEntity)e;
        execute();
    }

    @Override
    public boolean isCompatible(RootedEntity e) {
        setRootedEntity(e);
        if ( e == null ) {
            log.debug("Just nulled-out the rooted entity to LoadedWorkspaceCreator");
            return true;
        }
        else {
            log.debug("Just UN-Nulled rooted entity in LoadedWorkspaceCreator");            
            // Caching the test sampleEntity, for use in action label.
            final String entityTypeName = e.getEntity().getEntityTypeName();
            return entityTypeName.equals( EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE );
        }
    }

    @Override
    public String getActionLabel() {
        return "  Load Linux SWC Folder into New Workspace on Sample";
    }

    /**
     * @param rootedEntity the rootedEntity to set
     */
    private void setRootedEntity(RootedEntity rootedEntity) {
        this.rootedEntity = rootedEntity;
    }

    /**
     * @return the rootedEntity
     */
    private RootedEntity getRootedEntity() {
        return rootedEntity;
    }

}
