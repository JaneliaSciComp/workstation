package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for viewing details about a task.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskDetailsDialog extends ModalDialog {

    private static final Logger log = LoggerFactory.getLogger(TaskDetailsDialog.class);
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    protected static final int REFRESH_DELAY_MS = 2000;
    
    protected Long taskId;
    protected JPanel attrPanel;
    protected Timer refreshTimer;
    protected boolean closeUponTaskCompletion;
    protected JButton okButton;
   
    private JLabel addAttribute(String name) {
        JLabel nameLabel = new JLabel(name);
        JLabel valueLabel = new JLabel();
        nameLabel.setLabelFor(valueLabel);
        attrPanel.add(nameLabel);
        attrPanel.add(valueLabel);
        return valueLabel;
    }
    
    public TaskDetailsDialog() {
    	this(false);
    }
    
    public TaskDetailsDialog(boolean closeUponTaskCompletion) {

    	this.closeUponTaskCompletion = closeUponTaskCompletion;
        setTitle("Task Details");

    	add(Box.createHorizontalStrut(600), BorderLayout.NORTH);
    	
        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Task Properties")));

        add(attrPanel, BorderLayout.CENTER);

        okButton = new JButton("OK");
        okButton.setToolTipText("Close and refresh the main window");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				endRefresh();
				setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
            	endRefresh();
            }
        });
    }
    
    private void endRefresh() {
		log.info("Stopping refresh");
    	if (refreshTimer!=null) {
    		if (refreshTimer.isRunning()) {
    			refreshTimer.stop();
    		}
            refreshTimer = null;
            if (closeUponTaskCompletion) {
            	setVisible(false);
            }
    	}
    }

    public void showForTask(Task task) {

    	this.taskId = task.getObjectId();
    	updateForTask(task);
        packAndShow();
    }
    
    public void updateForTask(Task task) {
    	
    	attrPanel.removeAll();
    	
        addAttribute("Name: ").setText(task.getJobName());
        addAttribute("Task Owner: ").setText(task.getOwner());
        JLabel statusLabel = addAttribute("Last Status: ");
        statusLabel.setText(task.getLastEvent().getDescription());
        
        if (task.isDone()) {
        	if (task.getLastEvent().getEventType().equals(Event.ERROR_EVENT)) {
        		closeUponTaskCompletion = false;
        	}
    		endRefresh();
        }
        else {
        	statusLabel.setIcon(Icons.getLoadingIcon());
        
	        if (refreshTimer==null || !refreshTimer.isRunning()) {
	        	refreshTimer = new Timer(REFRESH_DELAY_MS, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							log.debug("Refresh "+taskId);
							final Task updatedTask = ModelMgr.getModelMgr().getTaskById(taskId);
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									updateForTask(updatedTask);	
								}
							});
						}
						catch (Exception error) {
							SessionMgr.getSessionMgr().handleException(error);
						}
					}
				});
	        	refreshTimer.setInitialDelay(REFRESH_DELAY_MS);
	        	refreshTimer.start(); 
	        	log.info("Starting refresh, every {} ms",REFRESH_DELAY_MS);
	        }
        }
        
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);
        repaint();
    }
}
