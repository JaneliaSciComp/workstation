/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 2:09 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.Timer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.TaskDetailsDialog;
import org.janelia.it.FlyWorkstation.gui.framework.console.Browser;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicColumn;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicRow;
import org.janelia.it.FlyWorkstation.gui.framework.table.DynamicTable;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;

/**
 * Provides a list of the user's Tasks and provides ways to manipulate and view them.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskOutline extends JPanel {

    private static final String COLUMN_NAME = "Name";
    private static final String COLUMN_STATUS = "Status";

    private static final int REFRESH_SECS = 10;
    
    private Browser consoleFrame;
    private JToggleButton hideCompletedButton;
    private final JPanel tablePanel;
    private final TaskDetailsDialog detailsDialog = new TaskDetailsDialog();
    private final JToolBar toolBar;

    private List<Task> tasks = new ArrayList<Task>();
    private Task selectedTask;
    private DynamicTable dynamicTable;
    private SimpleWorker loadingWorker;
    private Timer refreshTimer;

    private TableCellRenderer taskTableCellRenderer = new DefaultTableCellRenderer() {
        
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int rowIndex, int columnIndex) {
			JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
			DynamicRow row = dynamicTable.getRows().get(rowIndex);
			Task task = (Task)row.getUserObject();
			if (isSelected) {
				label.setForeground(table.getSelectionForeground());
			}
			else {
				if (task.isDone()) {
					label.setForeground(Color.gray);
				}
				else {
					label.setForeground(table.getForeground());
				}
			}
			return label;
		}
    };
    
    public TaskOutline(Browser consoleFrame) {
        super(new BorderLayout());

        this.consoleFrame = consoleFrame;

        tablePanel = new JPanel(new BorderLayout());
        add(tablePanel, BorderLayout.CENTER);

        toolBar = createToolBar();
        
        showLoadingIndicator();
        this.updateUI();
        loadTasks();
        
        TimerTask refreshTask = new TimerTask() {
			@Override
			public void run() {
		        loadTasks();
			}
		};
		
		refreshTimer = new Timer();
        refreshTimer.schedule(refreshTask, REFRESH_SECS*1000, REFRESH_SECS*1000);
    }

    public synchronized void showLoadingIndicator() {
        tablePanel.removeAll();
        tablePanel.add(new JLabel(Icons.getLoadingIcon()));
    }

    public synchronized void loadTasks() {

        loadingWorker = new SimpleWorker() {

        	List<Task> myTasks;
        	
            protected void doStuff() throws Exception {
            	myTasks = ModelMgr.getModelMgr().getUserParentTasks();
            }

            protected void hadSuccess() {
                try {
                    initializeTable(myTasks);
                    // Must selectTaskById because the objects have changed
                    if (selectedTask != null) selectTaskById(selectedTask.getObjectId());
                }
                catch (Exception e) {
                    hadError(e);
                }
                loadingWorker = null;
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                tablePanel.removeAll();
                TaskOutline.this.updateUI();
                loadingWorker = null;
            }

        };

        loadingWorker.execute();
    }
    
	public synchronized void initializeTable(List<Task> myTasks) {

        tasks.clear();

        dynamicTable = new DynamicTable() {
        	
            @Override
			public Object getValue(Object userObject, DynamicColumn column) {

            	Task task = (Task)userObject;
            	if (column.getName().equals(COLUMN_NAME)) {
            		return task.getJobName();
            	}
            	if (column.getName().equals(COLUMN_STATUS)) {
            		return task.getLastEvent().getEventType();
            	}
				return null;
			}

			@Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {
            	JPopupMenu popupMenu = super.createPopupMenu(e);
            	
            	if (dynamicTable.getCurrentRow() == null) return popupMenu;

                Object o = dynamicTable.getCurrentRow().getUserObject();
                final Task task = (Task)o;

        		final ListSelectionModel lsm = dynamicTable.getTable().getSelectionModel();
        		if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) { 

        			// Select this task 
        			
    				try {
    					selectedTask = task;
    				} catch (ArrayIndexOutOfBoundsException x) {
    					x.printStackTrace();
    				}
    				
        			// Items available if only a single item is selected
        			
	                if (task instanceof ContinuousExecutionTask) {
	                	final ContinuousExecutionTask cet = (ContinuousExecutionTask)task;
	                	if (cet.isStillEnabled() && !cet.isDone()) {
			                JMenuItem deleteMenuItem = new JMenuItem("  Stop");
			                deleteMenuItem.addActionListener(new ActionListener() {
			                    public void actionPerformed(ActionEvent actionEvent) {
			                    	try {
				                    	ModelMgr.getModelMgr().stopContinuousExecution(cet);
				                        JOptionPane.showMessageDialog(consoleFrame, 
				                        		"Continuous execution will end after the current execution is complete", 
				                        		"Stopped", JOptionPane.INFORMATION_MESSAGE);
			                    	} 
			                    	catch (Exception e) {
			                    		e.printStackTrace();
				                        JOptionPane.showMessageDialog(consoleFrame, 
				                        		"Error stopping continuous execution "+e.getMessage(), 
				                        		"Error", JOptionPane.ERROR_MESSAGE);
			                    	}
			                    }
			                });
			                popupMenu.add(deleteMenuItem);
	                	}
	                }

	                JMenuItem deleteMenuItem = new JMenuItem("  View details");
	                deleteMenuItem.addActionListener(new ActionListener() {
	                    public void actionPerformed(ActionEvent actionEvent) {
	    					detailsDialog.showForTask(selectedTask);
	                    }
	                });
	                popupMenu.add(deleteMenuItem);
	                
                }
                
//        		ListSelectionModel lsm = dynamicTable.getTable().getSelectionModel();
//        		if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex() || !task.isDone()) {
//	                JMenuItem deleteMenuItem = new JMenuItem("  Cancel");
//	                deleteMenuItem.addActionListener(new ActionListener() {
//	                    public void actionPerformed(ActionEvent actionEvent) {
//	                        cancelTasks();
//	                    }
//	                });
//	                popupMenu.add(deleteMenuItem);
//            	}
                
                JMenuItem deleteMenuItem = new JMenuItem("  Delete");
                deleteMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteTasks();
                    }
                });
                popupMenu.add(deleteMenuItem);

                return popupMenu;
            }

			@Override
			protected void rowClicked(int row) {
				try {
					selectedTask = tasks.get(row);
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				}
			}

			@Override
			protected void rowDoubleClicked(int row) {
				try {
					selectedTask = tasks.get(row);
					detailsDialog.showForTask(selectedTask);
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				}
			}
			
        };

        DynamicColumn nameCol = dynamicTable.addColumn(COLUMN_NAME, true, false, false);
        DynamicColumn statusCol = dynamicTable.addColumn(COLUMN_STATUS, true, false, false);
        
        
        dynamicTable.setColumnRenderer(nameCol, taskTableCellRenderer);
        dynamicTable.setColumnRenderer(statusCol, taskTableCellRenderer);
        
        if (null != myTasks) {
        	// Reverse id order (basically reverse chronological order since the ids are time-based)
            Collections.sort(myTasks, new Comparator<Task>() {
    			@Override
    			public int compare(Task o1, Task o2) {
    				return o2.getObjectId().compareTo(o1.getObjectId());
    			}
    		});
	        for (Task task : myTasks) {
	            if (AnnotationSessionTask.TASK_NAME.equals(task.getTaskName())) continue;
	            if (hideCompletedButton.isSelected() && task.isDone()) continue;
	            dynamicTable.addRow(task);
	            tasks.add(task);
	        }
        }

        dynamicTable.updateTableModel();
        tablePanel.removeAll();
        tablePanel.add(toolBar, BorderLayout.NORTH);
        tablePanel.add(dynamicTable, BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }
	
	private JToolBar createToolBar() {

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(false);

        hideCompletedButton = new JToggleButton();
        hideCompletedButton.setSelected(true); // selected by default
        hideCompletedButton.setIcon(Icons.getIcon("page_white_go.png"));
        hideCompletedButton.setFocusable(false);
        hideCompletedButton.setToolTipText("Hide all tasks which are completed.");
        hideCompletedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		        loadTasks();
			}
		});
        toolBar.add(hideCompletedButton);

        return toolBar;
	}
	
//	private synchronized void cancelTasks() {
//
//        final List<Task> toCancel = new ArrayList<Task>();
//        for (int i : dynamicTable.getTable().getSelectedRows()) {
//        	Task task = tasks.get(i);
//            if (!task.getOwner().equals(SessionMgr.getUsername())) {
//                JOptionPane.showMessageDialog(consoleFrame, 
//                		"Only the owner may cancel a task", "Cannot Cancel", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//            toCancel.add(task);
//        }
//
//
//        int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, 
//        		"Are you sure you want to cancel the selected tasks? ", "Delete Tasks", JOptionPane.YES_NO_OPTION);
//        if (deleteConfirmation != 0) return;
//
//        try {
//            for (Task task : toCancel) {
//                ModelMgr.getModelMgr().cancelTaskById(task.getObjectId());
//            }
//            
//            loadTasks();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            JOptionPane.showMessageDialog(consoleFrame, 
//            		"Error canceling session", "Error", JOptionPane.ERROR_MESSAGE);
//        }
//	}
	
    private synchronized void deleteTasks() {
    	
        final List<Task> toDelete = new ArrayList<Task>();
        for (int i : dynamicTable.getTable().getSelectedRows()) {
        	Task task = tasks.get(i);
            if (!task.getOwner().equals(SessionMgr.getUsername())) {
                JOptionPane.showMessageDialog(consoleFrame, 
                		"Only the owner may delete a task", "Cannot Delete", JOptionPane.ERROR_MESSAGE);
                return;
            }
            toDelete.add(task);
        }

        int deleteConfirmation = JOptionPane.showConfirmDialog(consoleFrame, 
        		"Are you sure you want to delete the selected tasks? ", "Delete Tasks", JOptionPane.YES_NO_OPTION);
        if (deleteConfirmation != 0) return;

        try {
            for (Task task : toDelete) {
                if (task instanceof ContinuousExecutionTask) {
                	final ContinuousExecutionTask cet = (ContinuousExecutionTask)task;
                	if (cet.isStillEnabled()) {
                        JOptionPane.showMessageDialog(consoleFrame, 
                        		"Cannot delete active task "+task.getJobName(), "Error", JOptionPane.ERROR_MESSAGE);
                		continue;
                	}
                }
                ModelMgr.getModelMgr().deleteTaskById(task.getObjectId());
                dynamicTable.removeRow(dynamicTable.getRowForUserObject(task));
                tasks.remove(task);
            }
            SwingUtilities.updateComponentTreeUI(dynamicTable);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(consoleFrame, 
            		"Error deleting session", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Task getTaskById(long taskId) {
    	for(DynamicRow row : dynamicTable.getRows()) {
            Task task = (Task) row.getUserObject();
            if (task.getObjectId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    public void selectTaskById(long taskId) {
    	selectTask(getTaskById(taskId));
    }

    public synchronized void selectTask(Task task) {
		if (dynamicTable.navigateToRowWithObject(task)) {
			this.selectedTask = task;	
		}
    }
}
