package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.LargeVolumeDiscoveryTask;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.workers.TaskMonitoringWorker;

public class LargeVolumeSampleDiscoveryAction extends AbstractAction {
	
	public LargeVolumeSampleDiscoveryAction() {
	}

    @Override
    public void actionPerformed(ActionEvent event) {
    	
        // Let user decide if it's a go.
        int optionSelected = JOptionPane.showConfirmDialog(
                FrameworkImplProvider.getMainFrame(),
                "Launch Discovery Process Now?",
                "Launch Large Volume Sample Discovery",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        
        if (optionSelected == JOptionPane.OK_OPTION) {

        	try {
	            HashSet<TaskParameter> taskParameters = new HashSet<>();
	            String displayName = "Update the set of known Large Volume Samples";
	            final Task task = StateMgr.getStateMgr().submitJob(LargeVolumeDiscoveryTask.PROCESS_NAME, displayName, taskParameters);
	            // Launch another thread/worker to monitor the remote-running task.
	            TaskMonitoringWorker tmw = new TaskMonitoringWorker(task.getObjectId()) {
	                @Override
	                public String getName() {
	                    return "Discover Large Volume Samples";
	                }
	                @Override
	                protected void doStuff() throws Exception {
	                    setStatus("Executing");
	                    super.doStuff();
	                }
	            };
	            tmw.executeWithEvents();
        	}
        	catch (Exception e) {
                ConsoleApp.handleException(e);
        	}
        }
	}
}
