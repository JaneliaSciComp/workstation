package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.RandomNeuronGenerator;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.lvv.TileFormat.MicrometerXyz;
import org.janelia.it.jacs.shared.lvv.TileFormat.VoxelXyz;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.workers.BackgroundWorker;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.util.IdSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for generating random neurons in the LVV.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GenerateNeuronsDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(GenerateNeuronsDialog.class);
    
    private final AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
    private AnnotationModel annotationModel;
    
    private final JButton cancelButton;
    private final JButton okButton;
    private final JPanel buttonPane;
    private final GroupedKeyValuePanel attrPanel;
    private final JTextField neuronCountField;
    private final JTextField pointCountField;
    private final JTextField branchProbabilityField;
    private final JCheckBox serverCheckbox;

    private Integer neuronCount;
    private Integer meanPointsPerNeuron;
    private Float branchProbability;

    private TileFormat tileFormat;
       
    public GenerateNeuronsDialog() {
    	super(ConsoleApp.getMainFrame());
    	
    	if (annotationMgr==null) {
    	    throw new IllegalStateException("LVV needs to be opened before this dialog is opened");
    	}
    	
    	this.annotationModel = annotationMgr.getAnnotationModel();
    	
        setTitle("Generate Neurons");

        attrPanel = new GroupedKeyValuePanel();

        attrPanel.addSeparator("Parameters");

        neuronCountField = new JTextField("10000");
        attrPanel.addItem("Neuron count", neuronCountField, "width 100:150:200, grow");

        pointCountField = new JTextField("100");
        attrPanel.addItem("Mean points per neuron", pointCountField, "width 100:150:200, grow");

        branchProbabilityField = new JTextField("0.02");
        attrPanel.addItem("Branch probability", branchProbabilityField, "width 100:150:200, grow");

        serverCheckbox = new JCheckBox();
        serverCheckbox.setSelected(true);
        attrPanel.addItem("Run remotely", serverCheckbox);
        
        add(attrPanel, BorderLayout.CENTER);
        
        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this window");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        okButton = new JButton("Generate");
        okButton.setToolTipText("Generate neurons in current workspace");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (generate()) {
                        setVisible(false);
                    }
                }
                catch (Exception ex) {
                    FrameworkImplProvider.handleException(ex);
                }
            }
        });

        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public void showDialog() {        
        ActivityLogHelper.logUserAction("GenerateNeuronsDialog.showDialog");
        packAndShow();
    }
    
    private boolean generate() throws Exception {
        
        try {
            neuronCount = Integer.parseInt(neuronCountField.getText());
            meanPointsPerNeuron = Integer.parseInt(pointCountField.getText());
            branchProbability = Float.parseFloat(branchProbabilityField.getText());
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Inputs are improperly formatted: "+e.getMessage(), "Invalid inputs", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        LargeVolumeViewViewer lvvv = LargeVolumeViewerTopComponent.getInstance().getLvvv();
        tileFormat = lvvv.getQuadViewUi().getTileFormat();
        BoundingBox3d boundingBox = lvvv.getQuadViewUi().getBoundingBox();
        log.info("Bounding box (micrometers): {}", boundingBox);
        
        VoxelXyz min = tileFormat.voxelXyzForMicrometerXyzMatrix(new MicrometerXyz(boundingBox.getMin()));
        VoxelXyz max = tileFormat.voxelXyzForMicrometerXyzMatrix(new MicrometerXyz(boundingBox.getMax()));
        boundingBox = new BoundingBox3d(min.asVec3(), max.asVec3());
        
        // Make the box smaller, because most neurons will not show up in 
        // the periphery (there's probably not even an image there)
        boundingBox = new BoundingBox3d(
                new Vec3(
                        boundingBox.getMinX()+1000, 
                        boundingBox.getMinY()+1000, 
                        boundingBox.getMinZ()+1000), 
                new Vec3(
                        boundingBox.getMaxX()-1000, 
                        boundingBox.getMaxY()-1000, 
                        boundingBox.getMaxZ()-1000));

        log.info("Bounding box (voxels): {}", boundingBox);
        
        final TmWorkspace currentWorkspace = annotationModel.getCurrentWorkspace();
        final String taskName = "Generating "+neuronCount+" neurons";
        
        log.info("Generating {} neurons with {} mean points per neuron, with {} branching possibility", neuronCount, meanPointsPerNeuron, branchProbability);
        
        if (serverCheckbox.isSelected()) {

            Vec3 bmin = boundingBox.getMin();
            Vec3 bmax = boundingBox.getMax();
            
            String ownerKey = AccessManager.getSubjectKey();
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("workspace id", currentWorkspace.getId().toString(), null));
            taskParameters.add(new TaskParameter("owner key", ownerKey, null));
            taskParameters.add(new TaskParameter("neuron count", neuronCount+"", null));
            taskParameters.add(new TaskParameter("mean points per neuron", meanPointsPerNeuron+"", null));
            taskParameters.add(new TaskParameter("branch probability", branchProbability+"", null));
            taskParameters.add(new TaskParameter("bounding box min", bmin.x()+","+bmin.y()+","+bmin.z(), null));
            taskParameters.add(new TaskParameter("bounding box max", bmax.x()+","+bmax.y()+","+bmax.z(), null));

            String displayName = "Random Neuron Generation";
            Task task = StateMgr.getStateMgr().submitJob("RandomNeuronGeneration", displayName, taskParameters);

            TaskMonitoringWorker tmw = new TaskMonitoringWorker(task.getObjectId()) {
                @Override
                public String getName() {
                    return taskName;
                }
                
                @Override
                protected void doStuff() throws Exception {

                    setStatus("Grid execution");

                    // Wait until task is finished
                    super.doStuff();

                    throwExceptionIfCancelled();

                    setStatus("Done");
                }
            };
            tmw.setSuccessCallback(new Callable<Void>() {
                
                @Override
                public Void call() throws Exception {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            LargeVolumeViewerTopComponent.getInstance().openLargeVolumeViewer(currentWorkspace);
                        }
                    });
                    return null;
                }
            });
            tmw.executeWithEvents();
        }
        else {

            IdSource idSource = new IdSource((int)(neuronCount*meanPointsPerNeuron*2));
            final RandomNeuronGenerator generator = new RandomNeuronGenerator(idSource, boundingBox, meanPointsPerNeuron, branchProbability);
            BackgroundWorker worker = new BackgroundWorker() {

                @Override
                public String getName() {
                    return taskName;
                }

                @Override
                protected void doStuff() throws Exception {
                    
                    int index = 1;
                    int total = neuronCount;
                    
                    for(int i=0; i<neuronCount; i++) {
                        String neuronName = "Neuron "+index;
                        setStatus("Creating artificial "+neuronName);
                        CompletableFuture<TmNeuronMetadata> future = annotationModel.getNeuronManager().createTiledMicroscopeNeuron(currentWorkspace, neuronName);
                        TmNeuronMetadata neuron = future.get(2, TimeUnit.SECONDS);
                        generator.generateArtificialNeuronData(neuron);
                        annotationModel.getNeuronManager().saveNeuronData(neuron);
                        setProgress(index++, total);
                    }
                    
                    setStatus("Completed artificial neuron generation");
                }
            };
            worker.setSuccessCallback(new Callable<Void>() {
                
                @Override
                public Void call() throws Exception {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            annotationModel.postWorkspaceUpdate(null);
                        }
                    });
                    return null;
                }
            });
            worker.executeWithEvents();    
        }
        
        return true;
    }
    

}
