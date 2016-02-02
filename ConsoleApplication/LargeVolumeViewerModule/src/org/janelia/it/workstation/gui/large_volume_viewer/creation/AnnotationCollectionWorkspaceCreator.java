package org.janelia.it.workstation.gui.large_volume_viewer.creation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.janelia.it.jacs.model.IdSource;
import org.janelia.it.jacs.model.entity.Entity;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.nb_action.EntityWrapperCreator;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.SwcImportTask;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.shared.util.SystemInfo;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.workstation.shared.workers.TaskMonitoringWorker;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Use this with a Tile Microscope Annotation Collection object, to transfer
 * its contents to a newly-created workspace.
 * 
 * @author fosterl
 */
@ServiceProvider(service=EntityWrapperCreator.class,path=EntityWrapperCreator.LOOKUP_PATH)
public class AnnotationCollectionWorkspaceCreator implements EntityWrapperCreator {
    
    private static final Logger log = LoggerFactory.getLogger(AnnotationCollectionWorkspaceCreator.class);
    
    private RootedEntity rootedEntity;
    
    public void execute() {

        final JFrame mainFrame = SessionMgr.getMainFrame();

        SimpleWorker worker = new SimpleWorker() {
            
            @Override
            protected void doStuff() throws Exception {
                // Simple dialog: need a workspace name
                final JDialog inputDialog = new JDialog(mainFrame, true);
                final JLabel errorLabel = new JLabel("   ");
                errorLabel.setForeground(Color.red);
                final JLabel workspaceNameLabel = new JLabel("Workspace Name");
                final JTextField workspaceNameTextField = new JTextField();
                inputDialog.setTitle("Workspace Name");
                inputDialog.setLayout(new GridLayout(5, 1));
                inputDialog.add(workspaceNameLabel);
                inputDialog.add(workspaceNameTextField);
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
                okButton.setToolTipText("Make workspace.");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        String workspaceName = workspaceNameTextField.getText();
                        if (workspaceName == null  ||  workspaceName.trim().isEmpty()) {
                            errorLabel.setText("Please enter a workspace name");
                        }
                        else {
                            workspaceName = workspaceName.trim();
                            inputDialog.setVisible(false);
                            String ownerKey = SessionMgr.getSessionMgr().getSubject().getKey();
                            try {
                                // Fetch the sample id from the collection.
                                Long collectionId = rootedEntity.getEntityId();
                                Entity collectionEntity = rootedEntity.getEntity();
                                
                                List<EntityData> neurons = new ArrayList<EntityData>();
                                Long sampleId = null;
                                for (EntityData ed: collectionEntity.getEntityData()) {
                                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_COLLECTION_SAMPLE_ID)) {
                                        sampleId = Long.parseLong(ed.getValue());
                                    }
                                    else if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON)) {
                                        neurons.add(ed);
                                    }
                                }
                                
                                // Let's create the new workspace.
                                AnnotationModel annoModel = new AnnotationModel();
                                Entity workspacesFolder = annoModel.getOrCreateWorkspacesFolder();
                                if (workspacesFolder == null) {
                                    throw new Exception("Failed to obtain workspaces folder.");
                                }
                                ModelMgr modelMgr = ModelMgr.getModelMgr();
                                TmWorkspace workspace = modelMgr.createTiledMicroscopeWorkspace(workspacesFolder.getId(),
                                        sampleId, workspaceName, ownerKey);
                                if (workspace == null) {
                                    throw new Exception("Failed to create workspace " + workspaceName);
                                }
                                
                                // Let's add copies of all the neurons to the workspace.
                                // THIS is a light-duty version of this operation.
                                // It should be performed server-side, rather
                                // than here.  It will do, during testing.  I
                                // just want to be assured that the neurons
                                // look right.  LLF, 2/2/2016
                                Long workspaceId = workspace.getId();
                                BASE64Decoder decoder = new BASE64Decoder();
                                BASE64Encoder encoder = new BASE64Encoder();
                                TmProtobufExchanger exchanger = new TmProtobufExchanger();
                                IdSource idSource = new IdSource();
                                Entity workspaceEntity = modelMgr.getEntityById(workspace.getId());

                                for (EntityData neuronEntityData: neurons) {
                                    // Resuscitate the neuron.
                                    String rawVal = neuronEntityData.getValue();
                                    byte[] protoBytes = decoder.decodeBuffer(rawVal);
                                    TmNeuron tmNeuron = exchanger.deserializeNeuron(protoBytes);
                                    // Now, must modify the neuron, so it fits
                                    // in its new environment.
                                    tmNeuron.setWorkspaceId(workspaceId);
                                    Long neuronId = idSource.next();
                                    tmNeuron.setId(neuronId);
                                    for (TmGeoAnnotation anno: tmNeuron.getGeoAnnotationMap().values()) {
                                        if (anno.getParentId() == anno.getNeuronId()) {
                                            anno.setParentId(neuronId);
                                        }
                                        anno.setNeuronId(neuronId);
                                    }
                                    
                                    // Prepare data for serialization.
                                    protoBytes = exchanger.serializeNeuron(tmNeuron);
                                    rawVal = encoder.encodeBuffer(protoBytes);
                                    
                                    // And now, write the neuron back.
                                    // This will be a fully-new copy of the neuron's data.  Intentionally duplicated.
                                    EntityData wsNeuronEntityData = new EntityData();
                                    wsNeuronEntityData.setId(neuronId);
                                    wsNeuronEntityData.setValue(rawVal);
                                    wsNeuronEntityData.setCreationDate(new Date());
                                    wsNeuronEntityData.setEntityAttrName(neuronEntityData.getEntityAttrName());
                                    wsNeuronEntityData.setOwnerKey(ownerKey);
                                    wsNeuronEntityData.setParentEntity(workspaceEntity);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                SessionMgr.getSessionMgr().handleException(ex);
                            }

                        }
                    }
                });
                buttonPanel.add(okButton, SystemInfo.isMac ? BorderLayout.LINE_END : BorderLayout.LINE_START);
                
                inputDialog.setSize(500, 180);
                inputDialog.setLocationRelativeTo(mainFrame);
                inputDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                inputDialog.setVisible(true);
                
                
//                if (userInput != null) {
//                    String ownerKey = SessionMgr.getSessionMgr().getSubject().getKey();
//                    // Expect the sample to be the 'main entity' of the LVV, since there is
//                    // no workspace.                                                        
//                    try {
//                        Long sampleId = rootedEntity.getEntityId();
//                        HashSet<TaskParameter> taskParameters = new HashSet<>();
//                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_sampleId, sampleId.toString(), null));
//                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_userName, ownerKey, null));
//                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_workspaceName, workspaceNameTextField.getText().trim(), null));
//                        taskParameters.add(new TaskParameter(SwcImportTask.PARAM_topLevelFolderName, userInput, null));
//
//                        String taskName = new File(userInput).getName();
//                        String displayName = taskName + " for 3D tiled microscope sample " + sampleId;
//                        final Task task = ModelMgr.getModelMgr().submitJob(SwcImportTask.PROCESS_NAME, displayName, taskParameters);
//
//                        // Launch another thread/worker to monitor the 
//                        // remote-running task.
//                        TaskMonitoringWorker tmw = new TaskMonitoringWorker(task.getObjectId()) {
//                            @Override
//                            public void doStuff() throws Exception {
//                                super.doStuff();
//                            }
//                            @Override
//                            public String getName() {
//                                if (userInput != null) {
//                                    File uiFile = new File(userInput);
//                                    return "import all SWCs in " + uiFile.getName();
//                                } else {
//                                    return "import SWC for sample";
//                                }
//                            }
//                            
//                        };
//                        tmw.executeWithEvents();
//                        
//                    } catch (Exception e) {
//                        String errorString = "Error launching : " + e.getMessage();
//                        log.error(errorString);
//                        throw e;
//                    }
//                }
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
            log.debug("Just nulled-out the rooted entity");
            return true;
        }
        else {
            log.debug("Just UN-Nulled rooted entity");            
            // Caching the test entity, for use in action label.
            final String entityTypeName = e.getEntity().getEntityTypeName();
            return entityTypeName.equals( EntityConstants.TYPE_ANNOTATION_COLLECTION );
        }
    }

    @Override
    public String getActionLabel() {
        return "  Make a Workspace with Annotation Collection";
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

    private static class PathCorrectionKeyListener implements KeyListener {
        private JTextField pathTextField;
        public PathCorrectionKeyListener(JTextField pathTextField) {
            this.pathTextField = pathTextField;
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            char keyChar = e.getKeyChar();
            if (keyChar == '\\') {
                // Trim the backslash off the end, and add back
                // a front-slash.
                pathTextField.setText(
                        pathTextField.getText().substring(0, pathTextField.getText().length() - 1) + '/'
                );
            }
        }

    }

}
