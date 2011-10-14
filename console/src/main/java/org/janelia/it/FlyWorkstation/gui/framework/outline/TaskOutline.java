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
    
    private List<Task> tasks = new ArrayList<Task>();
    private Task selectedTask;
    private Browser consoleFrame;
    protected final JPanel tablePanel;
    private DynamicTable dynamicTable;
    private SimpleWorker loadingWorker;
    private Timer refreshTimer;
    private TaskDetailsDialog detailsDialog = new TaskDetailsDialog();

    private TableCellRenderer taskTableCellRenderer = new DefaultTableCellRenderer() {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int rowIndex, int columnIndex) {
			JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
			DynamicRow row = dynamicTable.getRows().get(rowIndex);
			Task task = (Task)row.getUserObject();
			if (task.isDone()) {
				label.setForeground(Color.gray);
			}
			else {
				label.setForeground(Color.black);
			}
			return label;
		}
    };
    
    public TaskOutline(Browser consoleFrame) {
        super(new BorderLayout());

        this.consoleFrame = consoleFrame;

        tablePanel = new JPanel(new BorderLayout());
        add(tablePanel, BorderLayout.CENTER);

        showLoadingIndicator();
        this.updateUI();
        loadTasks();
        
        TimerTask refreshTask = new TimerTask() {
			@Override
			public void run() {
				System.out.println("Reloading task outline");
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
                    if (selectedTask != null) dynamicTable.navigateToRowWithObject(selectedTask);
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
            		return task.getDisplayName();
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

//                JMenuItem editMenuItem = new JMenuItem("  View details");
//                editMenuItem.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent actionEvent) {
//                    	// TODO: view details
//                    }
//                });
//                popupMenu.add(editMenuItem);

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
			                    		// TODO: this two lines should probably be done atomically on the server side
				                    	cet.setEnabled(false);
				                    	ContinuousExecutionTask updated = (ContinuousExecutionTask)ModelMgr.getModelMgr().saveOrUpdateTask(task);
				                    	System.out.println("Updated task "+updated.getObjectId()+" enabled?="+updated.isStillEnabled());

				                        JOptionPane.showMessageDialog(consoleFrame, 
				                        		"Continuous execution will be stopped before the next run begins.", 
				                        		"Stopped", JOptionPane.INFORMATION_MESSAGE);
			                    	} 
			                    	catch (Exception e) {
			                    		e.printStackTrace();
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
	            dynamicTable.addRow(task);
	            tasks.add(task);
	        }
        }

        dynamicTable.updateTableModel();
        tablePanel.removeAll();
        tablePanel.add(dynamicTable);
        
        revalidate();
        repaint();
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
    	this.selectedTask = task;
		dynamicTable.navigateToRowWithObject(task);
    }
}
