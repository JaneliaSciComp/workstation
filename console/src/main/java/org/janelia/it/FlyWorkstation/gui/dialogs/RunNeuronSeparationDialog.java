package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOUnifiedFileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * A dialog for starting a continuous neuron separation pipeline task which runs every N minutes and discovers new files
 * to run neuron separation on. Once the task is started, it can be managed with the TaskOutline. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunNeuronSeparationDialog extends ModalDialog {

	private static final String INPUT_DIR = "/groups/flylight/flylight/%USER%/data";
	private static final String TOP_LEVEL_FOLDER_NAME = "%USER%'s Single Neuron Data";
	private static final String LINKING_DIR_TEMPLATE = "/groups/scicomp/jacsData/flylight/%USER%/MySeparationResultLinks";
	private static final int DEFAULT_RERUN_INTERVAL_MINS = 60;
	private static final int DEFAULT_STATUS_CHECK_INTERVAL_SECS = 30;
	
	private static final String TOOLTIP_INPUT_DIR = "Root directory of the tree that should be loaded into the database";
	private static final String TOOLTIP_LINKING_DIR = "Directory where symbolic links to the results should be created";
	private static final String TOOLTIP_TOP_LEVEL_ENTITY = "Name of the database entity which should be loaded with the data";
	private static final String TOOLTIP_RERUN_INTERVAL = "Once a run is complete, how soon should we re-run it?";
	private static final String TOOLTIP_REFRESH = "Run a new separation for samples that already have a separation result?";
	
    private final JPanel attrPanel;    
    private final JTextField inputDirectoryField;
    private final JTextField linkingDirectoryField;
    private final JTextField topLevelFolderField;
    private final JTextField rerunIntervalField;
    private final JCheckBox refreshCheckbox;
    
    public RunNeuronSeparationDialog() {
    	
        setTitle("Launch Periodic Neuron Separation Task");
        
        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Task Parameters")));

        JLabel nameLabel = new JLabel("Input Directory (Linux mounted)");
        nameLabel.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryField = new JTextField(40);
        inputDirectoryField.setText(filter(INPUT_DIR));
        inputDirectoryField.setToolTipText(TOOLTIP_INPUT_DIR);
        nameLabel.setLabelFor(inputDirectoryField);
        attrPanel.add(nameLabel);
        attrPanel.add(inputDirectoryField);

        JLabel nameLabel5 = new JLabel("Linking Directory (Linux mounted)");
        nameLabel5.setToolTipText(TOOLTIP_LINKING_DIR);
        linkingDirectoryField = new JTextField(40);
        linkingDirectoryField.setText(filter(LINKING_DIR_TEMPLATE));
        linkingDirectoryField.setToolTipText(TOOLTIP_LINKING_DIR);
        nameLabel5.setLabelFor(linkingDirectoryField);
        attrPanel.add(nameLabel5);
        attrPanel.add(linkingDirectoryField);

        JLabel nameLabel6 = new JLabel("Top Level Entity Name");
        nameLabel6.setToolTipText(TOOLTIP_TOP_LEVEL_ENTITY);
        topLevelFolderField = new JTextField(40);
        topLevelFolderField.setText(filter(TOP_LEVEL_FOLDER_NAME));
        topLevelFolderField.setToolTipText(TOOLTIP_TOP_LEVEL_ENTITY);
        nameLabel6.setLabelFor(topLevelFolderField);
        attrPanel.add(nameLabel6);
        attrPanel.add(topLevelFolderField);
        
        JLabel nameLabel2 = new JLabel("Re-run Interval (minutes)");
        nameLabel2.setToolTipText(TOOLTIP_RERUN_INTERVAL);
        rerunIntervalField = new JTextField(10);
        rerunIntervalField.setText(DEFAULT_RERUN_INTERVAL_MINS+"");
        rerunIntervalField.setToolTipText(TOOLTIP_RERUN_INTERVAL);
        nameLabel2.setLabelFor(rerunIntervalField);
        attrPanel.add(nameLabel2);
        attrPanel.add(rerunIntervalField);

        JLabel nameLabel3 = new JLabel("Re-run samples with existing results?");
        nameLabel3.setToolTipText(TOOLTIP_REFRESH);
        refreshCheckbox = new JCheckBox();
        refreshCheckbox.setToolTipText(TOOLTIP_REFRESH);
        nameLabel3.setLabelFor(refreshCheckbox);
        attrPanel.add(nameLabel3);
        attrPanel.add(refreshCheckbox);
        
        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);
        
        JButton okButton = new JButton("Run");
        okButton.setToolTipText("Run the neuron separation");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runNeuronSeparation();
			}
		});
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }
     
    public void runNeuronSeparation() {
    	
    	Utils.setWaitingCursor(this);
    	
    	final boolean refresh = refreshCheckbox.isSelected();
    	final String inputDirPath = inputDirectoryField.getText();
    	final String linkingDirName = linkingDirectoryField.getText();
    	final String topLevelFolderName = topLevelFolderField.getText();
    	
    	int rerunMins = 0;
    	try {
    		rerunMins = Integer.parseInt(rerunIntervalField.getText());
    	}
    	catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
            		"Can't parse refresh interval", "Error", JOptionPane.ERROR_MESSAGE);
    	}
    	
    	final int loopTimerInMinutes = rerunMins;
    	
    	SimpleWorker executeWorker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				startSeparation(inputDirPath, linkingDirName, topLevelFolderName, loopTimerInMinutes, refresh);
			}
			
			@Override
			protected void hadSuccess() {
		    	Utils.setDefaultCursor(RunNeuronSeparationDialog.this);
	            Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
	            browser.getTaskOutline().loadTasks();
	            browser.getOutlookBar().setVisibleBarByName(Browser.BAR_TASKS);
				setVisible(false);
			}
			
			@Override
			protected void hadError(Throwable error) {
				error.printStackTrace();
		    	Utils.setDefaultCursor(RunNeuronSeparationDialog.this);
	            JOptionPane.showMessageDialog(RunNeuronSeparationDialog.this, 
	            		"Error submitting job: "+error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		};
    	
		executeWorker.execute();
    }
    
    public void showDialog() {
        packAndShow();
    }

    /**
     * Begin the separation task, wrapped in a continuous execution task.
     * @param path root directory to use for file discovery
     * @param loopTimerInMinutes how long to wait before re-running
     * @param stitched run the stitched version of the pipeline?
     * @param refresh refresh entities which were already created?
     */
    private void startSeparation(String path, String linkingDirName, String topLevelFolderName,
    		int loopTimerInMinutes, boolean refresh) {

    	try {
    		String inputDirList = path;
        	String process = null;
        	Task task = null;
        	Set<Node> inputNodes = new HashSet<Node>();
        	String owner = SessionMgr.getUsername();
        	List<Event> events = new ArrayList<Event>();
    		Set<TaskParameter> taskParameterSet = new HashSet<TaskParameter>();

            process = "MCFOUnifiedFileDiscovery";
            task = new MCFOUnifiedFileDiscoveryTask(inputNodes, owner, events, taskParameterSet, 
            		inputDirList, topLevelFolderName, linkingDirName, refresh);
            task.setParameter(MCFOUnifiedFileDiscoveryTask.PARAM_inputDirectoryList, inputDirList);
            task.setJobName("MultiColor FlipOut File Discovery Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
            
    		Task ceTask = new ContinuousExecutionTask(new HashSet<Node>(), 
    				owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), loopTimerInMinutes, 
            		true, task.getObjectId(), process, DEFAULT_STATUS_CHECK_INTERVAL_SECS);

    		ceTask.setJobName("Continuous Neuron Separation Service");
    		ceTask = ModelMgr.getModelMgr().saveOrUpdateTask(ceTask);

            ModelMgr.getModelMgr().submitJob("ContinuousExecution", ceTask.getObjectId());
    	}
    	catch (Exception e) {
    		SessionMgr.getSessionMgr().handleException(e);
    	}
    }

    private String filter(String s) {
    	return s.replaceAll("%USER%", SessionMgr.getUsername());
    }
}
