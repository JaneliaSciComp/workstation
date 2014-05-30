package org.janelia.it.workstation.gui.dialogs;

import loci.plugins.config.SpringUtilities;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.gui.framework.console.Perspective;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFODataPipelineTask;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;
import org.janelia.it.jacs.model.user_data.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A dialog for starting a continuous neuron separation pipeline task which runs every N minutes and discovers new files
 * to run neuron separation on. Once the task is started, it can be managed with the TaskOutline. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunNeuronSeparationDialog extends ModalDialog {

	public static final String PREF_NEURON_SERVICE_INPUT_DIR    =           "NeuronSeparationService.InputDir";
    public static final String PREF_NEURON_SERVICE_FOLDER_NAME  =           "NeuronSeparationService.TopLevelFolderName";
    public static final String PREF_NEURON_SERVICE_RERUN_INTERVAL_VALUE =   "NeuronSeparationService.ReRunValue";
    public static final String PREF_NEURON_SERVICE_RERUN_SCALE          =   "NeuronSeparationService.ReRunScale";
    public static final String SCALE_DAYS   = "Days";
    public static final String SCALE_MINUTES= "Minutes";
    
    private static final String INPUT_DIR = "";
	private static final String TOP_LEVEL_FOLDER_NAME = "%USER%'s Single Neuron Data";
	private static final String DEFAULT_RERUN_INTERVAL_VALUE = "1";
	private static final int DEFAULT_STATUS_CHECK_INTERVAL_SECS = 30;
	
	private static final String TOOLTIP_DATA_SET        = "Name of the data set to add discovered samples to";
	private static final String TOOLTIP_INPUT_DIR       = "Root directory of the tree that should be loaded into the database";
	private static final String TOOLTIP_TOP_LEVEL_FOLDER= "Name of the folder which should be loaded with the data";
	private static final String TOOLTIP_RERUN_INTERVAL  = "Once a run is complete, how soon should we re-run it?";
	private static final String TOOLTIP_REFRESH         = "Run a new separation for samples that already have a separation result?";
    private static final String TOOLTIP_CONTINUOUSLY    = "Continuously look for new LSM Pairs until I say stop";

    private final JPanel attrPanel;    
    private final JTextField inputDirectoryField;
    private final JTextField topLevelFolderField;
    private final JTextField rerunIntervalField;
    private final JComboBox dataSetCombo;
    private final ButtonGroup scaleGroup;
    private final JRadioButton dayRadioButton;
    private final JRadioButton minuteRadioButton;
    private final JCheckBox refreshCheckbox;
    private final JCheckBox runContinuouslyCheckBox;
    
    public RunNeuronSeparationDialog() {
    	
        setTitle("Launch Neuron Separation");
        
        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Task Parameters")));

        JLabel topLevelFolderLabel = new JLabel("Top Level Folder Name");
        topLevelFolderLabel.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderField = new JTextField(40);
        topLevelFolderField.setText(filter(TOP_LEVEL_FOLDER_NAME));
        topLevelFolderField.setToolTipText(TOOLTIP_TOP_LEVEL_FOLDER);
        topLevelFolderLabel.setLabelFor(topLevelFolderField);
        attrPanel.add(topLevelFolderLabel);
        attrPanel.add(topLevelFolderField);

        JLabel inputDirectoryLabel = new JLabel("Input Directory (Linux mounted)");
        inputDirectoryLabel.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryField = new JTextField(40);
        inputDirectoryField.setText(filter(INPUT_DIR));
        inputDirectoryField.setToolTipText(TOOLTIP_INPUT_DIR);
        inputDirectoryLabel.setLabelFor(inputDirectoryField);
        attrPanel.add(inputDirectoryLabel);
        attrPanel.add(inputDirectoryField);


        JLabel dataSetLabel = new JLabel("Data Set");
        dataSetLabel.setToolTipText(TOOLTIP_DATA_SET);
        dataSetCombo = new JComboBox();
        dataSetCombo.setToolTipText(TOOLTIP_DATA_SET);
        dataSetLabel.setLabelFor(dataSetCombo);
        attrPanel.add(dataSetLabel);
        attrPanel.add(dataSetCombo);
        
        JLabel rerunIntervalLabel = new JLabel("Re-run Interval");
        rerunIntervalLabel.setToolTipText(TOOLTIP_RERUN_INTERVAL);
        rerunIntervalField = new JTextField(5);
        rerunIntervalField.setText(DEFAULT_RERUN_INTERVAL_VALUE + "");
        rerunIntervalField.setToolTipText(TOOLTIP_RERUN_INTERVAL);
        rerunIntervalLabel.setLabelFor(rerunIntervalField);
        dayRadioButton = new JRadioButton("Day(s)");
        minuteRadioButton = new JRadioButton("Minute(s)");
        dayRadioButton.setSelected(true);
        scaleGroup = new ButtonGroup();
        scaleGroup.add(minuteRadioButton);
        scaleGroup.add(dayRadioButton);
        JPanel rerunPanel = new JPanel();
        rerunPanel.setLayout(new BoxLayout(rerunPanel, BoxLayout.X_AXIS));
        rerunPanel.add(rerunIntervalField);
        rerunPanel.add(minuteRadioButton);
        rerunPanel.add(dayRadioButton);
        attrPanel.add(rerunIntervalLabel);
        attrPanel.add(rerunPanel);

        JLabel refreshLabel = new JLabel("Re-run samples with existing results?");
        refreshLabel.setToolTipText(TOOLTIP_REFRESH);
        refreshCheckbox = new JCheckBox();
        refreshCheckbox.setToolTipText(TOOLTIP_REFRESH);
        refreshLabel.setLabelFor(refreshCheckbox);
        attrPanel.add(refreshLabel);
        attrPanel.add(refreshCheckbox);
        
        JLabel continuousLabel = new JLabel("Run Continuously");
        continuousLabel.setToolTipText(TOOLTIP_CONTINUOUSLY);
        runContinuouslyCheckBox = new JCheckBox();
        runContinuouslyCheckBox.setToolTipText(TOOLTIP_CONTINUOUSLY);
        continuousLabel.setLabelFor(runContinuouslyCheckBox);
        attrPanel.add(continuousLabel);
        attrPanel.add(runContinuouslyCheckBox);

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
        
        // Get the user prefs and set
        String userFolderName = (String)SessionMgr.getSessionMgr().getModelProperty(PREF_NEURON_SERVICE_FOLDER_NAME);
        String userInputDir   = (String)SessionMgr.getSessionMgr().getModelProperty(PREF_NEURON_SERVICE_INPUT_DIR);
        String userRerunValue = (String)SessionMgr.getSessionMgr().getModelProperty(PREF_NEURON_SERVICE_RERUN_INTERVAL_VALUE);
        String userRerunScale = (String)SessionMgr.getSessionMgr().getModelProperty(PREF_NEURON_SERVICE_RERUN_SCALE);
        if (null!=userFolderName) { topLevelFolderField.setText(userFolderName); }
        if (null!=userInputDir)   { inputDirectoryField.setText(userInputDir); }
        if (null!=userRerunValue) { 
            rerunIntervalField.setText(userRerunValue); 
        }
        else {
            rerunIntervalField.setText("1");
        }
        if (null!=userRerunScale) {
            if (SCALE_DAYS.equals(userRerunScale)) { dayRadioButton.setSelected(true); }
            else if (SCALE_MINUTES.equals(userRerunScale)) {
                minuteRadioButton.setSelected(true);}
            else { dayRadioButton.setSelected(true); }
        }
    }
     
    public void runNeuronSeparation() {
    	
    	Utils.setWaitingCursor(this);
    	
    	final boolean refresh = refreshCheckbox.isSelected();
    	final String inputDirPath = inputDirectoryField.getText();
    	final String topLevelFolderName = topLevelFolderField.getText();
    	final boolean runContinously = runContinuouslyCheckBox.isSelected();

    	Entity dataSet = (Entity)dataSetCombo.getSelectedItem();
    	final String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    	
    	int rerunMins = 0;
    	try {
    		rerunMins = Integer.parseInt(rerunIntervalField.getText());
            if (dayRadioButton.isSelected()) {
                rerunMins = rerunMins * 1440;
            }
    	}
    	catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
            		"Can't parse refresh interval", "Error", JOptionPane.ERROR_MESSAGE);
    	}
    	
        // Update user Preferences
        if (null!=topLevelFolderField.getText()) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_NEURON_SERVICE_FOLDER_NAME,topLevelFolderField.getText());
        }
        if (null!=inputDirectoryField.getText()) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_NEURON_SERVICE_INPUT_DIR,inputDirectoryField.getText());
        }
        if (null!=rerunIntervalField.getText()) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_NEURON_SERVICE_RERUN_INTERVAL_VALUE,rerunIntervalField.getText());
        }
        if (dayRadioButton.isSelected()) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_NEURON_SERVICE_RERUN_SCALE,SCALE_DAYS);
        }
        else if (minuteRadioButton.isSelected()) {
            SessionMgr.getSessionMgr().setModelProperty(PREF_NEURON_SERVICE_RERUN_SCALE,SCALE_MINUTES);
        }

        final int loopTimerInMinutes = rerunMins;
    	
    	SimpleWorker executeWorker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				startSeparation(inputDirPath, null, topLevelFolderName, dataSetIdentifier, loopTimerInMinutes, refresh, runContinously);
			}
			
			@Override
			protected void hadSuccess() {
		    	Utils.setDefaultCursor(RunNeuronSeparationDialog.this);
	            Browser browser = SessionMgr.getBrowser();
                browser.setPerspective(Perspective.TaskMonitoring);
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
    	
    	SimpleWorker worker = new SimpleWorker() {
    		List<Entity> dataSets;
			@Override
			protected void doStuff() throws Exception {
				dataSets = ModelMgr.getModelMgr().getDataSets();
			}
			@Override
			protected void hadSuccess() {
				dataSetCombo.removeAllItems();
				for(Entity dataSetEntity : dataSets) {
					dataSetCombo.addItem(dataSetEntity);
				}
			}
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		worker.execute();
    	
        packAndShow();
    }

    /**
     * Begin the separation task, wrapped in a continuous execution task.
     * @param path root directory to use for file discovery
     * @param loopTimerInMinutes how long to wait before re-running
     * @param refresh refresh entities which were already created?
     */
    private void startSeparation(String path, String linkingDirName, String topLevelFolderName, String dataSetIdentifier, 
    		int loopTimerInMinutes, boolean refresh, boolean runContinously) {

    	try {
    		String inputDirList = path;
        	String process;
        	Task task;
        	String owner = SessionMgr.getSubjectKey();
        	
            process = "NMSDataPipeline";
            task = new MCFODataPipelineTask(new HashSet<Node>(), owner, new ArrayList<Event>(), 
            		new HashSet<TaskParameter>(), inputDirList, topLevelFolderName, false, false, refresh, null);
            task.setParameter("run mode", "INCOMPLETE");
            task.setParameter("data set identifier", dataSetIdentifier);
            task.setJobName("Neuron Merge Separation Task");
            task = ModelMgr.getModelMgr().saveOrUpdateTask(task);

            if (runContinously) {
                Task ceTask = new ContinuousExecutionTask(new HashSet<Node>(),
                        owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), loopTimerInMinutes,
                        true, task.getObjectId(), process, DEFAULT_STATUS_CHECK_INTERVAL_SECS);

                ceTask.setJobName("Continuous Neuron Separation Service");
                ceTask = ModelMgr.getModelMgr().saveOrUpdateTask(ceTask);

                ModelMgr.getModelMgr().submitJob("ContinuousExecution", ceTask);
            }
            else {
                ModelMgr.getModelMgr().submitJob(process, task);
            }
        }
    	catch (Exception e) {
    		SessionMgr.getSessionMgr().handleException(e);
    	}
    }

    private String filter(String s) {
    	return s.replaceAll("%USER%", SessionMgr.getUsername());
    }
}
